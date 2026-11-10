package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Servico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    Optional<Servico> findByNomeIgnoreCase(String nome);

    List<Servico> findAllByOrderByNomeAsc();

    List<Servico> findByAtivoTrueOrderByNomeAsc();
    Optional<Servico> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    Optional<Servico> findByNomeIgnoreCaseAndEstabelecimentoId(String nome, Long estabelecimentoId);

    List<Servico> findAllByEstabelecimentoIdOrderByNomeAsc(Long estabelecimentoId);

    List<Servico> findByEstabelecimentoIdAndAtivoTrueOrderByNomeAsc(Long estabelecimentoId);
    List<Servico> findAllByEstabelecimentoIdAndIdIn(Long estabelecimentoId, List<Long> ids);
}

