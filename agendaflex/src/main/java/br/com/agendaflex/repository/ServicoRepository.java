package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Servico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    Optional<Servico> findByNomeIgnoreCase(String nome);

    List<Servico> findAllByOrderByNomeAsc();

    List<Servico> findByAtivoTrueOrderByNomeAsc();
}
