package br.com.agendaflex.service;

import br.com.agendaflex.dtos.HorarioTrabalhoDTO;
import br.com.agendaflex.entity.HorarioTrabalho;
import br.com.agendaflex.entity.Profissional;
import br.com.agendaflex.exception.RecursoNaoEncontradoException;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.HorarioTrabalhoRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HorarioTrabalhoService {

    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final ProfissionalRepository profissionalRepository;

    /** Ordenado de segunda a domingo e, dentro do dia, pelo horário inicial. */
    public List<HorarioTrabalhoDTO> listarPorProfissional(Long profissionalId) {
        return horarioTrabalhoRepository.findByProfissionalId(profissionalId).stream()
                .sorted(Comparator.comparing((HorarioTrabalho h) -> h.getDiaSemana().getValue())
                        .thenComparing(HorarioTrabalho::getHoraInicio))
                .map(this::paraDTO)
                .toList();
    }

    @Transactional
    public HorarioTrabalhoDTO adicionar(HorarioTrabalhoDTO dto) {
        Profissional profissional = profissionalRepository.findById(dto.getProfissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", dto.getProfissionalId()));

        if (!dto.getHoraFim().isAfter(dto.getHoraInicio())) {
            throw new RegraNegocioException("A hora final precisa ser depois da hora inicial.");
        }

        boolean sobrepoe = horarioTrabalhoRepository
                .findByProfissionalIdAndDiaSemanaOrderByHoraInicio(profissional.getId(), dto.getDiaSemana())
                .stream()
                .anyMatch(h -> h.sobrepoe(dto.getHoraInicio(), dto.getHoraFim()));
        if (sobrepoe) {
            throw new RegraNegocioException("Esse horário se sobrepõe a outro já cadastrado para o mesmo dia.");
        }

        HorarioTrabalho horario = new HorarioTrabalho(
                profissional, dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFim());
        return paraDTO(horarioTrabalhoRepository.save(horario));
    }

    @Transactional
    public void remover(Long id) {
        HorarioTrabalho horario = horarioTrabalhoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Horário de trabalho", id));
        horarioTrabalhoRepository.delete(horario);
    }

    private HorarioTrabalhoDTO paraDTO(HorarioTrabalho h) {
        HorarioTrabalhoDTO dto = new HorarioTrabalhoDTO();
        dto.setId(h.getId());
        dto.setProfissionalId(h.getProfissional().getId());
        dto.setDiaSemana(h.getDiaSemana());
        dto.setHoraInicio(h.getHoraInicio());
        dto.setHoraFim(h.getHoraFim());
        return dto;
    }
}
