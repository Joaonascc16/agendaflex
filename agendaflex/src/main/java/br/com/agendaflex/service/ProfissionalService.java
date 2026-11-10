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
    private final TenantContext tenantContext;

    public List<ProfissionalDTO> listar() {
        return profissionalRepository.findAllByEstabelecimentoIdOrderByNomeAsc(tenantContext.obterEstabelecimentoId()).stream().map(this::paraDTO).toList();
    }

    public List<ProfissionalDTO> listarAtivos() {
        return profissionalRepository.findByEstabelecimentoIdAndAtivoTrueOrderByNomeAsc(tenantContext.obterEstabelecimentoId()).stream().map(this::paraDTO).toList();
    }

    /** Profissionais ativos que realizam o serviço informado. */
    public List<ProfissionalDTO> listarPorServico(Long servicoId) {
        return profissionalRepository.buscarAtivosPorServicoNoEstabelecimento(tenantContext.obterEstabelecimentoId(), servicoId).stream().map(this::paraDTO).toList();
    }

    public ProfissionalDTO buscar(Long id) {
        return paraDTO(buscarEntidade(id));
    }

    @Transactional
    public ProfissionalDTO salvar(ProfissionalDTO dto) {
        Profissional profissional = dto.getId() == null ? new Profissional() : buscarEntidade(dto.getId());

        // Regra: não dá para desativar quem ainda tem atendimentos marcados.
        if (profissional.getId() != null && profissional.isAtivo() && !dto.isAtivo()
                && agendamentoRepository.existsByProfissionalIdAndEstabelecimentoIdAndStatusInAndInicioAfter(
                        profissional.getId(), tenantContext.obterEstabelecimentoId(),
                        StatusAgendamento.ATIVOS, LocalDateTime.now(clock))) {
            throw new RegraNegocioException("Este profissional ainda tem agendamentos futuros. "
                    + "Reagende ou cancele esses atendimentos antes de desativá-lo.");
        }

        if (dto.getId() == null) {
            profissional.setEstabelecimento(tenantContext.obterEstabelecimento());
        }
        profissional.setNome(dto.getNome().trim());
        profissional.setEspecialidade(TextoUtil.limparOuNulo(dto.getEspecialidade()));
        profissional.setAtivo(dto.isAtivo());

        List<Long> ids = dto.getServicosIds() == null ? List.of() : dto.getServicosIds();
        List<Long> idsUnicos = ids.stream().distinct().toList();
        List<Servico> servicosDoEstabelecimento = idsUnicos.isEmpty() ? List.of()
                : servicoRepository.findAllByEstabelecimentoIdAndIdIn(tenantContext.obterEstabelecimentoId(), idsUnicos);
        if (servicosDoEstabelecimento.size() != idsUnicos.size()) {
            throw new RegraNegocioException("Um ou mais serviços não pertencem a este estabelecimento.");
        }
        profissional.getServicos().clear();
        profissional.getServicos().addAll(servicosDoEstabelecimento);

        return paraDTO(profissionalRepository.save(profissional));
    }

    @Transactional
    public void excluir(Long id) {
        Profissional profissional = buscarEntidade(id);
        if (agendamentoRepository.existsByProfissionalIdAndEstabelecimentoId(id, tenantContext.obterEstabelecimentoId())) {
            throw new RegraNegocioException("Este profissional tem histórico de agendamentos e não pode ser excluído. "
                    + "Desative o cadastro para que ele deixe de receber novos agendamentos.");
        }
        horarioTrabalhoRepository.deleteByEstabelecimentoIdAndProfissionalId(tenantContext.obterEstabelecimentoId(), id);
        bloqueioRepository.deleteByEstabelecimentoIdAndProfissionalId(tenantContext.obterEstabelecimentoId(), id);
        profissionalRepository.delete(profissional);
    }

    private Profissional buscarEntidade(Long id) {
        return profissionalRepository.findByIdAndEstabelecimentoId(id, tenantContext.obterEstabelecimentoId())
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
