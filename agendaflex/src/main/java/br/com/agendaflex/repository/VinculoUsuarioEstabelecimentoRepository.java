package br.com.agendaflex.repository;

import br.com.agendaflex.entity.VinculoUsuarioEstabelecimento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface VinculoUsuarioEstabelecimentoRepository
        extends JpaRepository<VinculoUsuarioEstabelecimento, Long> {

    @EntityGraph(attributePaths = "estabelecimento")
    Optional<VinculoUsuarioEstabelecimento> findByUsuarioIdAndEstabelecimentoId(Long usuarioId,
                                                                                 Long estabelecimentoId);

    @EntityGraph(attributePaths = "estabelecimento")
    List<VinculoUsuarioEstabelecimento> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    List<VinculoUsuarioEstabelecimento> findByEstabelecimentoIdAndAtivoTrue(Long estabelecimentoId);
}
