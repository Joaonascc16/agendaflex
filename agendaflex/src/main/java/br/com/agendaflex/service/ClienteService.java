package br.com.agendaflex.service;

import br.com.agendaflex.config.RegrasAgendamentoProperties;
import br.com.agendaflex.dtos.ClienteDTO;
import br.com.agendaflex.entity.Cliente;
import br.com.agendaflex.exception.RecursoNaoEncontradoException;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.ClienteRepository;
import br.com.agendaflex.util.TextoUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final RegrasAgendamentoProperties regras;

    public List<ClienteDTO> listar(String busca) {
        List<Cliente> clientes;
        if (busca == null || busca.isBlank()) {
            clientes = clienteRepository.findAllByOrderByNomeAsc();
        } else {
            String termo = busca.trim();
            clientes = clienteRepository
                    .findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNomeAsc(termo, termo);
        }
        return clientes.stream().map(this::paraDTO).toList();
    }

    public List<ClienteDTO> listarAtivos() {
        return clienteRepository.findByAtivoTrueOrderByNomeAsc().stream().map(this::paraDTO).toList();
    }

    public ClienteDTO buscar(Long id) {
        return paraDTO(buscarEntidade(id));
    }

    @Transactional
    public ClienteDTO salvar(ClienteDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();

        clienteRepository.findByEmailIgnoreCase(email).ifPresent(existente -> {
            if (dto.getId() == null || !existente.getId().equals(dto.getId())) {
                throw new RegraNegocioException("Já existe um cliente cadastrado com esse e-mail.");
            }
        });

        Cliente cliente = dto.getId() == null ? new Cliente() : buscarEntidade(dto.getId());
        cliente.setNome(dto.getNome().trim());
        cliente.setEmail(email);
        cliente.setTelefone(TextoUtil.limparOuNulo(dto.getTelefone()));
        cliente.setAtivo(dto.isAtivo());
        return paraDTO(clienteRepository.save(cliente));
    }

    /** Clientes com histórico de agendamentos não são apagados: apenas desativados. */
    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidade(id);
        if (agendamentoRepository.existsByClienteId(id)) {
            throw new RegraNegocioException("Este cliente tem histórico de agendamentos e não pode ser excluído. "
                    + "Desative o cadastro para que ele deixe de aparecer nos novos agendamentos.");
        }
        clienteRepository.delete(cliente);
    }

    @Transactional
    public void zerarFaltas(Long id) {
        buscarEntidade(id).zerarFaltas();
    }

    private Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    private ClienteDTO paraDTO(Cliente c) {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(c.getId());
        dto.setNome(c.getNome());
        dto.setEmail(c.getEmail());
        dto.setTelefone(c.getTelefone());
        dto.setAtivo(c.isAtivo());
        dto.setFaltas(c.getFaltas());
        dto.setBloqueado(c.estaBloqueado(regras.getLimiteFaltasParaBloqueio()));
        return dto;
    }
}
