package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Profissional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findAllByOrderByNomeAsc();

    List<Profissional> findByAtivoTrueOrderByNomeAsc();

    /** Profissionais ativos habilitados para um serviço. */
    @Query("""
           select distinct p from Profissional p
           join p.servicos s
           where s.id = :servicoId and p.ativo = true
           order by p.nome
           """)
    List<Profissional> buscarAtivosPorServico(@Param("servicoId") Long servicoId);

    boolean existsByServicosId(Long servicoId);
}
