package br.com.agendaflex.repository;

import br.com.agendaflex.entity.Agendamento;
import br.com.agendaflex.enums.StatusAgendamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // ------------------------------------------------------------------
    // Conflitos de horário (intervalo [inicio, fim) — encostar não conflita)
    // Use ignorarId = 0L para não ignorar nenhum agendamento.
    // ------------------------------------------------------------------

    @Query("""
           select a from Agendamento a
           where a.profissional.id = :profissionalId
             and a.status in :status
             and a.inicio < :fim and a.fim > :inicio
             and a.id <> :ignorarId
           """)
    List<Agendamento> buscarConflitosDoProfissional(@Param("profissionalId") Long profissionalId,
                                                    @Param("status") Collection<StatusAgendamento> status,
                                                    @Param("inicio") LocalDateTime inicio,
                                                    @Param("fim") LocalDateTime fim,
                                                    @Param("ignorarId") Long ignorarId);

    @Query("""
           select a from Agendamento a
           where a.cliente.id = :clienteId
             and a.status in :status
             and a.inicio < :fim and a.fim > :inicio
             and a.id <> :ignorarId
           """)
    List<Agendamento> buscarConflitosDoCliente(@Param("clienteId") Long clienteId,
                                               @Param("status") Collection<StatusAgendamento> status,
                                               @Param("inicio") LocalDateTime inicio,
                                               @Param("fim") LocalDateTime fim,
                                               @Param("ignorarId") Long ignorarId);

    // ------------------------------------------------------------------
    // Limites por cliente
    // ------------------------------------------------------------------

    long countByClienteIdAndStatusInAndInicioAfter(Long clienteId,
                                                   Collection<StatusAgendamento> status,
                                                   LocalDateTime referencia);

    @Query("""
           select count(a) from Agendamento a
           where a.cliente.id = :clienteId
             and a.status in :status
             and a.inicio >= :de and a.inicio < :ate
           """)
    long contarDoClienteNoPeriodo(@Param("clienteId") Long clienteId,
                                  @Param("status") Collection<StatusAgendamento> status,
                                  @Param("de") LocalDateTime de,
                                  @Param("ate") LocalDateTime ate);

    // ------------------------------------------------------------------
    // Listagens (carregam cliente, profissional e serviço de uma vez)
    // ------------------------------------------------------------------

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findAllByOrderByInicioDesc();

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByStatusOrderByInicioDesc(StatusAgendamento status);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByProfissionalIdOrderByInicioDesc(Long profissionalId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByStatusAndProfissionalIdOrderByInicioDesc(StatusAgendamento status, Long profissionalId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByClienteIdOrderByInicioDesc(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findTop5ByStatusInAndInicioAfterOrderByInicioAsc(Collection<StatusAgendamento> status,
                                                                      LocalDateTime referencia);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    @Query("""
           select a from Agendamento a
           where a.profissional.id = :profissionalId
             and a.inicio >= :de and a.inicio < :ate
           order by a.inicio
           """)
    List<Agendamento> buscarNoPeriodoDoProfissional(@Param("profissionalId") Long profissionalId,
                                                    @Param("de") LocalDateTime de,
                                                    @Param("ate") LocalDateTime ate);

    // ------------------------------------------------------------------
    // Indicadores do painel
    // ------------------------------------------------------------------

    @Query("""
           select count(a) from Agendamento a
           where a.status in :status
             and a.inicio >= :de and a.inicio < :ate
           """)
    long contarPorStatusNoPeriodo(@Param("status") Collection<StatusAgendamento> status,
                                  @Param("de") LocalDateTime de,
                                  @Param("ate") LocalDateTime ate);

    /** Retorna null quando não há registros no período. */
    @Query("""
           select sum(a.precoCobrado) from Agendamento a
           where a.status = :status
             and a.inicio >= :de and a.inicio < :ate
           """)
    BigDecimal somarReceita(@Param("status") StatusAgendamento status,
                            @Param("de") LocalDateTime de,
                            @Param("ate") LocalDateTime ate);

    // ------------------------------------------------------------------
    // Verificações antes de excluir cadastros
    // ------------------------------------------------------------------

    boolean existsByClienteId(Long clienteId);

    boolean existsByProfissionalId(Long profissionalId);

    boolean existsByServicoId(Long servicoId);

    /** Há atendimentos ativos futuros para o profissional? */
    boolean existsByProfissionalIdAndStatusInAndInicioAfter(Long profissionalId,
                                                            Collection<StatusAgendamento> status,
                                                            LocalDateTime referencia);

    // ------------------------------------------------------------------
    // Consultas com isolamento por estabelecimento (usar nos serviços da aplicação)
    // ------------------------------------------------------------------

    @Query("""
           select a from Agendamento a
           where a.estabelecimento.id = :estabelecimentoId
             and a.profissional.id = :profissionalId
             and a.status in :status
             and a.inicio < :fim and a.fim > :inicio
             and a.id <> :ignorarId
           """)
    List<Agendamento> buscarConflitosDoProfissionalNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("profissionalId") Long profissionalId,
            @Param("status") Collection<StatusAgendamento> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("ignorarId") Long ignorarId);

    @Query("""
           select a from Agendamento a
           where a.estabelecimento.id = :estabelecimentoId
             and a.cliente.id = :clienteId
             and a.status in :status
             and a.inicio < :fim and a.fim > :inicio
             and a.id <> :ignorarId
           """)
    List<Agendamento> buscarConflitosDoClienteNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("clienteId") Long clienteId,
            @Param("status") Collection<StatusAgendamento> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("ignorarId") Long ignorarId);

    long countByClienteIdAndEstabelecimentoIdAndStatusInAndInicioAfter(
            Long clienteId, Long estabelecimentoId, Collection<StatusAgendamento> status,
            LocalDateTime referencia);

    @Query("""
           select count(a) from Agendamento a
           where a.estabelecimento.id = :estabelecimentoId
             and a.cliente.id = :clienteId
             and a.status in :status
             and a.inicio >= :de and a.inicio < :ate
           """)
    long contarDoClienteNoPeriodoNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("clienteId") Long clienteId,
            @Param("status") Collection<StatusAgendamento> status,
            @Param("de") LocalDateTime de,
            @Param("ate") LocalDateTime ate);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findAllByEstabelecimentoIdOrderByInicioDesc(Long estabelecimentoId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByEstabelecimentoIdAndStatusOrderByInicioDesc(
            Long estabelecimentoId, StatusAgendamento status);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByEstabelecimentoIdAndProfissionalIdOrderByInicioDesc(
            Long estabelecimentoId, Long profissionalId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByEstabelecimentoIdAndStatusAndProfissionalIdOrderByInicioDesc(
            Long estabelecimentoId, StatusAgendamento status, Long profissionalId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findByEstabelecimentoIdAndClienteIdOrderByInicioDesc(
            Long estabelecimentoId, Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    List<Agendamento> findTop5ByEstabelecimentoIdAndStatusInAndInicioAfterOrderByInicioAsc(
            Long estabelecimentoId, Collection<StatusAgendamento> status, LocalDateTime referencia);

    @EntityGraph(attributePaths = {"cliente", "profissional", "servico"})
    @Query("""
           select a from Agendamento a
           where a.estabelecimento.id = :estabelecimentoId
             and a.profissional.id = :profissionalId
             and a.inicio >= :de and a.inicio < :ate
           order by a.inicio
           """)
    List<Agendamento> buscarNoPeriodoDoProfissionalNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("profissionalId") Long profissionalId,
            @Param("de") LocalDateTime de,
            @Param("ate") LocalDateTime ate);

    @Query("""
           select count(a) from Agendamento a
           where a.estabelecimento.id = :estabelecimentoId
             and a.status in :status
             and a.inicio >= :de and a.inicio < :ate
           """)
    long contarPorStatusNoPeriodoNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("status") Collection<StatusAgendamento> status,
            @Param("de") LocalDateTime de,
            @Param("ate") LocalDateTime ate);

    @Query("""
           select sum(a.precoCobrado) from Agendamento a
           where a.estabelecimento.id = :estabelecimentoId
             and a.status = :status
             and a.inicio >= :de and a.inicio < :ate
           """)
    BigDecimal somarReceitaNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("status") StatusAgendamento status,
            @Param("de") LocalDateTime de,
            @Param("ate") LocalDateTime ate);

    Optional<Agendamento> findByIdAndEstabelecimentoId(Long id, Long estabelecimentoId);

    boolean existsByClienteIdAndEstabelecimentoId(Long clienteId, Long estabelecimentoId);

    boolean existsByProfissionalIdAndEstabelecimentoId(Long profissionalId, Long estabelecimentoId);

    boolean existsByServicoIdAndEstabelecimentoId(Long servicoId, Long estabelecimentoId);

    boolean existsByProfissionalIdAndEstabelecimentoIdAndStatusInAndInicioAfter(
            Long profissionalId, Long estabelecimentoId,
            Collection<StatusAgendamento> status, LocalDateTime referencia);

}
