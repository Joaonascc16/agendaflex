package br.com.agendaflex.repository;

import br.com.agendaflex.entity.HorarioTrabalho;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    List<HorarioTrabalho> findByProfissionalIdAndDiaSemanaOrderByHoraInicio(Long profissionalId, DayOfWeek diaSemana);

    List<HorarioTrabalho> findByProfissionalId(Long profissionalId);

    void deleteByProfissionalId(Long profissionalId);
}
