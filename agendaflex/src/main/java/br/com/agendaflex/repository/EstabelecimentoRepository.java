package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Estabelecimento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    Optional<Estabelecimento> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
