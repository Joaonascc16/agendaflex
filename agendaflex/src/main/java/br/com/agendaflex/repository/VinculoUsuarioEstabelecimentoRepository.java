package br.com.agendaflex.repository;

import br.com.agendaflex.entity.VinculoUsuarioEstabelecimento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoUsuarioEstabelecimentoRepository
        extends JpaRepository<VinculoUsuarioEstabelecimento, Long> {

    Optional<VinculoUsuarioEstabelecimento> findByUsuarioIdAndEstabelecimentoId(Long usuarioId,
                                                                                 Long estabelecimentoId);

    List<VinculoUsuarioEstabelecimento> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    List<VinculoUsuarioEstabelecimento> findByEstabelecimentoIdAndAtivoTrue(Long estabelecimentoId);
}
