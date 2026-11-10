package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    List<Cliente> findAllByOrderByNomeAsc();

    List<Cliente> findByAtivoTrueOrderByNomeAsc();

    List<Cliente> findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNomeAsc(String nome, String email);
    Optional<Cliente> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    Optional<Cliente> findByEmailIgnoreCaseAndEstabelecimentoId(String email, Long estabelecimentoId);

    List<Cliente> findAllByEstabelecimentoIdOrderByNomeAsc(Long estabelecimentoId);

    List<Cliente> findByEstabelecimentoIdAndAtivoTrueOrderByNomeAsc(Long estabelecimentoId);

    @Query("""
           select c from Cliente c
           where c.estabelecimento.id = :estabelecimentoId
             and (lower(c.nome) like lower(concat('%', :termo, '%'))
                  or lower(c.email) like lower(concat('%', :termo, '%')))
           order by c.nome
           """)
    List<Cliente> buscarPorTermoNoEstabelecimento(@Param("estabelecimentoId") Long estabelecimentoId,
                                                   @Param("termo") String termo);
}
