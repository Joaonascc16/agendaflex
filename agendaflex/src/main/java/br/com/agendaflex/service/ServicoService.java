package br.com.agendaflex.service;

import br.com.agendaflex.dtos.ServicoDTO;
import br.com.agendaflex.entity.Servico;
import br.com.agendaflex.exception.RecursoNaoEncontradoException;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import br.com.agendaflex.repository.ServicoRepository;
import br.com.agendaflex.util.TextoUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final TenantContext tenantContext;

    public List<ServicoDTO> listar() {
        return servicoRepository.findAllByEstabelecimentoIdOrderByNomeAsc(tenantContext.obterEstabelecimentoId()).stream().map(this::paraDTO).toList();
    }

    public List<ServicoDTO> listarAtivos() {
        return servicoRepository.findByEstabelecimentoIdAndAtivoTrueOrderByNomeAsc(tenantContext.obterEstabelecimentoId()).stream().map(this::paraDTO).toList();
    }

    public ServicoDTO buscar(Long id) {
        return paraDTO(buscarEntidade(id));
    }

    @Transactional
    public ServicoDTO salvar(ServicoDTO dto) {
        String nome = dto.getNome().trim();

        servicoRepository.findByNomeIgnoreCaseAndEstabelecimentoId(nome, tenantContext.obterEstabelecimentoId()).ifPresent(existente -> {
            if (dto.getId() == null || !existente.getId().equals(dto.getId())) {
                throw new RegraNegocioException("Já existe um serviço com esse nome.");
            }
        });

        Servico servico = dto.getId() == null ? new Servico() : buscarEntidade(dto.getId());
        if (dto.getId() == null) {
            servico.setEstabelecimento(tenantContext.obterEstabelecimento());
        }
        servico.setNome(nome);
        servico.setDescricao(TextoUtil.limparOuNulo(dto.getDescricao()));
        servico.setDuracaoMinutos(dto.getDuracaoMinutos());
        servico.setPreco(dto.getPreco());
        servico.setAtivo(dto.isAtivo());
        return paraDTO(servicoRepository.save(servico));
    }

    @Transactional
    public void excluir(Long id) {
        Servico servico = buscarEntidade(id);
        if (agendamentoRepository.existsByServicoIdAndEstabelecimentoId(id, tenantContext.obterEstabelecimentoId())) {
            throw new RegraNegocioException("Este serviço já foi usado em agendamentos e não pode ser excluído. "
                    + "Desative-o para que ele deixe de ser oferecido.");
        }
        if (profissionalRepository.existsByEstabelecimentoIdAndServicosId(tenantContext.obterEstabelecimentoId(), id)) {
            throw new RegraNegocioException("Remova o serviço dos profissionais que o realizam antes de excluí-lo.");
        }
        servicoRepository.delete(servico);
    }

    private Servico buscarEntidade(Long id) {
        return servicoRepository.findByIdAndEstabelecimentoId(id, tenantContext.obterEstabelecimentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }

    private ServicoDTO paraDTO(Servico s) {
        ServicoDTO dto = new ServicoDTO();
        dto.setId(s.getId());
        dto.setNome(s.getNome());
        dto.setDescricao(s.getDescricao());
        dto.setDuracaoMinutos(s.getDuracaoMinutos());
        dto.setPreco(s.getPreco());
        dto.setAtivo(s.isAtivo());
        return dto;
    }
}
