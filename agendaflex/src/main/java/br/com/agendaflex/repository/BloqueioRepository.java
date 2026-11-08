package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Bloqueio;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    /** Bloqueios do profissional que se sobrepõem ao intervalo [inicio, fim). */
    @Query("""
           select b from Bloqueio b
           where b.profissional.id = :profissionalId
             and b.inicio < :fim and b.fim > :inicio
           order by b.inicio
           """)
    List<Bloqueio> buscarSobrepostos(@Param("profissionalId") Long profissionalId,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim);

    /** Bloqueios que ainda não terminaram. */
    List<Bloqueio> findByProfissionalIdAndFimAfterOrderByInicio(Long profissionalId, LocalDateTime referencia);

    void deleteByProfissionalId(Long profissionalId);
}
