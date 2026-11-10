package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Profissional;
import java.util.List;
import java.util.Optional;
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
    Optional<Profissional> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    List<Profissional> findAllByEstabelecimentoIdOrderByNomeAsc(Long estabelecimentoId);

    List<Profissional> findByEstabelecimentoIdAndAtivoTrueOrderByNomeAsc(Long estabelecimentoId);

    @Query("""
           select distinct p from Profissional p
           join p.servicos s
           where p.estabelecimento.id = :estabelecimentoId
             and s.estabelecimento.id = :estabelecimentoId
             and s.id = :servicoId and p.ativo = true
           order by p.nome
           """)
    List<Profissional> buscarAtivosPorServicoNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("servicoId") Long servicoId);

    List<Profissional> findAllByEstabelecimentoIdAndIdIn(Long estabelecimentoId, List<Long> ids);

    boolean existsByEstabelecimentoIdAndServicosId(Long estabelecimentoId, Long servicoId);
}
