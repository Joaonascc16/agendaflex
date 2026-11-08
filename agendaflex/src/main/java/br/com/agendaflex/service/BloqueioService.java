package br.com.agendaflex.service;

import br.com.agendaflex.dtos.BloqueioDTO;
import br.com.agendaflex.entity.Agendamento;
import br.com.agendaflex.entity.Bloqueio;
import br.com.agendaflex.entity.Profissional;
import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.exception.RecursoNaoEncontradoException;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.BloqueioRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import br.com.agendaflex.util.TextoUtil;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BloqueioService {

    private final BloqueioRepository bloqueioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final Clock clock;

    /** Bloqueios que ainda não terminaram. */
    public List<BloqueioDTO> listarVigentes(Long profissionalId) {
        return bloqueioRepository
                .findByProfissionalIdAndFimAfterOrderByInicio(profissionalId, LocalDateTime.now(clock))
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional
    public BloqueioDTO criar(BloqueioDTO dto) {
        Profissional profissional = profissionalRepository.findById(dto.getProfissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", dto.getProfissionalId()));

        if (!dto.getFim().isAfter(dto.getInicio())) {
            throw new RegraNegocioException("O fim do bloqueio precisa ser depois do início.");
        }
        if (!dto.getFim().isAfter(LocalDateTime.now(clock))) {
            throw new RegraNegocioException("O bloqueio precisa terminar no futuro.");
        }

        List<Agendamento> conflitos = agendamentoRepository.buscarConflitosDoProfissional(
                profissional.getId(), StatusAgendamento.ATIVOS, dto.getInicio(), dto.getFim(), 0L);
        if (!conflitos.isEmpty()) {
            throw new RegraNegocioException("Existem " + conflitos.size() + " agendamento(s) ativo(s) nesse período. "
                    + "Reagende ou cancele esses atendimentos antes de bloquear a agenda.");
        }

        Bloqueio bloqueio = new Bloqueio(profissional, dto.getInicio(), dto.getFim(), dto.getMotivo().trim());
        return paraDTO(bloqueioRepository.save(bloqueio));
    }

    @Transactional
    public void remover(Long id) {
        Bloqueio bloqueio = bloqueioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloqueio", id));
        bloqueioRepository.delete(bloqueio);
    }

    private BloqueioDTO paraDTO(Bloqueio b) {
        BloqueioDTO dto = new BloqueioDTO();
        dto.setId(b.getId());
        dto.setProfissionalId(b.getProfissional().getId());
        dto.setInicio(b.getInicio());
        dto.setFim(b.getFim());
        dto.setMotivo(TextoUtil.limparOuNulo(b.getMotivo()));
        return dto;
    }
}
