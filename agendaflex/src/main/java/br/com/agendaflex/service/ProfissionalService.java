package br.com.agendaflex.service;

import br.com.agendaflex.dtos.ProfissionalDTO;
import br.com.agendaflex.entity.Profissional;
import br.com.agendaflex.entity.Servico;
import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.exception.RecursoNaoEncontradoException;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.BloqueioRepository;
import br.com.agendaflex.repository.HorarioTrabalhoRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import br.com.agendaflex.repository.ServicoRepository;
import br.com.agendaflex.util.TextoUtil;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final Clock clock;

    public List<ProfissionalDTO> listar() {
        return profissionalRepository.findAllByOrderByNomeAsc().stream().map(this::paraDTO).toList();
    }

    public List<ProfissionalDTO> listarAtivos() {
        return profissionalRepository.findByAtivoTrueOrderByNomeAsc().stream().map(this::paraDTO).toList();
    }

    /** Profissionais ativos que realizam o serviço informado. */
    public List<ProfissionalDTO> listarPorServico(Long servicoId) {
        return profissionalRepository.buscarAtivosPorServico(servicoId).stream().map(this::paraDTO).toList();
    }

    public ProfissionalDTO buscar(Long id) {
        return paraDTO(buscarEntidade(id));
    }

    @Transactional
    public ProfissionalDTO salvar(ProfissionalDTO dto) {
        Profissional profissional = dto.getId() == null ? new Profissional() : buscarEntidade(dto.getId());

        // Regra: não dá para desativar quem ainda tem atendimentos marcados.
        if (profissional.getId() != null && profissional.isAtivo() && !dto.isAtivo()
                && agendamentoRepository.existsByProfissionalIdAndStatusInAndInicioAfter(
                        profissional.getId(), StatusAgendamento.ATIVOS, LocalDateTime.now(clock))) {
            throw new RegraNegocioException("Este profissional ainda tem agendamentos futuros. "
                    + "Reagende ou cancele esses atendimentos antes de desativá-lo.");
        }

        profissional.setNome(dto.getNome().trim());
        profissional.setEspecialidade(TextoUtil.limparOuNulo(dto.getEspecialidade()));
        profissional.setAtivo(dto.isAtivo());

        List<Long> ids = dto.getServicosIds() == null ? List.of() : dto.getServicosIds();
        profissional.getServicos().clear();
        profissional.getServicos().addAll(servicoRepository.findAllById(ids));

        return paraDTO(profissionalRepository.save(profissional));
    }

    @Transactional
    public void excluir(Long id) {
        Profissional profissional = buscarEntidade(id);
        if (agendamentoRepository.existsByProfissionalId(id)) {
            throw new RegraNegocioException("Este profissional tem histórico de agendamentos e não pode ser excluído. "
                    + "Desative o cadastro para que ele deixe de receber novos agendamentos.");
        }
        horarioTrabalhoRepository.deleteByProfissionalId(id);
        bloqueioRepository.deleteByProfissionalId(id);
        profissionalRepository.delete(profissional);
    }

    private Profissional buscarEntidade(Long id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", id));
    }

    private ProfissionalDTO paraDTO(Profissional p) {
        ProfissionalDTO dto = new ProfissionalDTO();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setEspecialidade(p.getEspecialidade());
        dto.setAtivo(p.isAtivo());
        dto.setServicosIds(p.getServicos().stream().map(Servico::getId).toList());
        dto.setServicosNomes(p.getServicos().stream()
                .map(Servico::getNome)
                .sorted(Comparator.naturalOrder())
                .toList());
        return dto;
    }
}
