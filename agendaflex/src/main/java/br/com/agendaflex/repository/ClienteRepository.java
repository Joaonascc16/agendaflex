package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    List<Cliente> findAllByOrderByNomeAsc();

    List<Cliente> findByAtivoTrueOrderByNomeAsc();

    List<Cliente> findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNomeAsc(String nome, String email);
}
