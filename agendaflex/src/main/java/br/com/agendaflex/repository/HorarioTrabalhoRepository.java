package br.com.agendaflex.repository;

import br.com.agendaflex.entity.HorarioTrabalho;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    List<HorarioTrabalho> findByProfissionalIdAndDiaSemanaOrderByHoraInicio(Long profissionalId, DayOfWeek diaSemana);

    List<HorarioTrabalho> findByProfissionalId(Long profissionalId);

    void deleteByProfissionalId(Long profissionalId);
    List<HorarioTrabalho> findByEstabelecimentoIdAndProfissionalIdAndDiaSemanaOrderByHoraInicio(
            Long estabelecimentoId, Long profissionalId, DayOfWeek diaSemana);

    List<HorarioTrabalho> findByEstabelecimentoIdAndProfissionalId(Long estabelecimentoId, Long profissionalId);

    Optional<HorarioTrabalho> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    void deleteByEstabelecimentoIdAndProfissionalId(Long estabelecimentoId, Long profissionalId);
}
