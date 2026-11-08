package br.com.agendaflex.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.agendaflex.entity.Agendamento;
import br.com.agendaflex.entity.Cliente;
import br.com.agendaflex.entity.Profissional;
import br.com.agendaflex.entity.Servico;
import br.com.agendaflex.enums.StatusAgendamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** Garante que as consultas JPQL de conflito e de indicadores funcionam de verdade no banco. */
@DataJpaTest
class AgendamentoRepositoryTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);

    @Autowired private TestEntityManager em;
    @Autowired private AgendamentoRepository repository;

    private Cliente cliente;
    private Profissional profissional;
    private Servico servico;

    @BeforeEach
    void preparar() {
        cliente = em.persist(new Cliente("Maria", "maria@email.com", null));
        profissional = em.persist(new Profissional("Ana", "Cabeleireira"));
        servico = em.persist(new Servico("Corte", null, 60, new BigDecimal("80.00")));
    }

    private Agendamento salvar(LocalDateTime inicio, LocalDateTime fim, StatusAgendamento status, String preco) {
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setStatus(status);
        agendamento.setPrecoCobrado(new BigDecimal(preco));
        return em.persistAndFlush(agendamento);
    }

    @Test
    @DisplayName("Encontra agendamento que se sobrepõe ao intervalo consultado")
    void encontraConflitoDeProfissional() {
        salvar(DIA.atTime(10, 0), DIA.atTime(11, 0), StatusAgendamento.AGENDADO, "80.00");

        var conflitos = repository.buscarConflitosDoProfissional(profissional.getId(), StatusAgendamento.ATIVOS,
                DIA.atTime(10, 30), DIA.atTime(11, 30), 0L);

        assertThat(conflitos).hasSize(1);
    }

    @Test
    @DisplayName("Horários colados, um logo após o outro, não são conflito")
    void horariosColadosNaoConflitam() {
        salvar(DIA.atTime(10, 0), DIA.atTime(11, 0), StatusAgendamento.AGENDADO, "80.00");

        var depois = repository.buscarConflitosDoProfissional(profissional.getId(), StatusAgendamento.ATIVOS,
                DIA.atTime(11, 0), DIA.atTime(12, 0), 0L);
        var antes = repository.buscarConflitosDoProfissional(profissional.getId(), StatusAgendamento.ATIVOS,
                DIA.atTime(9, 0), DIA.atTime(10, 0), 0L);

        assertThat(depois).isEmpty();
        assertThat(antes).isEmpty();
    }

    @Test
    @DisplayName("Agendamentos cancelados não ocupam a agenda")
    void canceladosNaoConflitam() {
        salvar(DIA.atTime(10, 0), DIA.atTime(11, 0), StatusAgendamento.CANCELADO, "80.00");

        var conflitos = repository.buscarConflitosDoProfissional(profissional.getId(), StatusAgendamento.ATIVOS,
                DIA.atTime(10, 0), DIA.atTime(11, 0), 0L);

        assertThat(conflitos).isEmpty();
    }

    @Test
    @DisplayName("O agendamento ignorado não conta como conflito (usado no reagendamento)")
    void ignoraOProprioAgendamento() {
        Agendamento existente = salvar(DIA.atTime(10, 0), DIA.atTime(11, 0), StatusAgendamento.AGENDADO, "80.00");

        var conflitos = repository.buscarConflitosDoProfissional(profissional.getId(), StatusAgendamento.ATIVOS,
                DIA.atTime(10, 0), DIA.atTime(11, 0), existente.getId());

        assertThat(conflitos).isEmpty();
    }

    @Test
    @DisplayName("Encontra conflito de horário do cliente")
    void encontraConflitoDeCliente() {
        salvar(DIA.atTime(10, 0), DIA.atTime(11, 0), StatusAgendamento.CONFIRMADO, "80.00");

        var conflitos = repository.buscarConflitosDoCliente(cliente.getId(), StatusAgendamento.ATIVOS,
                DIA.atTime(10, 30), DIA.atTime(11, 30), 0L);

        assertThat(conflitos).hasSize(1);
    }

    @Test
    @DisplayName("Conta apenas agendamentos ativos do cliente no dia")
    void contaAgendamentosAtivosDoClienteNoDia() {
        salvar(DIA.atTime(9, 0), DIA.atTime(10, 0), StatusAgendamento.AGENDADO, "80.00");
        salvar(DIA.atTime(14, 0), DIA.atTime(15, 0), StatusAgendamento.CANCELADO, "80.00");
        salvar(DIA.plusDays(1).atTime(9, 0), DIA.plusDays(1).atTime(10, 0), StatusAgendamento.AGENDADO, "80.00");

        long noDia = repository.contarDoClienteNoPeriodo(cliente.getId(), StatusAgendamento.ATIVOS,
                DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay());

        assertThat(noDia).isEqualTo(1);
    }

    @Test
    @DisplayName("Soma a receita apenas dos atendimentos concluídos no período")
    void somaReceitaDosConcluidos() {
        salvar(DIA.atTime(9, 0), DIA.atTime(10, 0), StatusAgendamento.CONCLUIDO, "80.00");
        salvar(DIA.atTime(11, 0), DIA.atTime(12, 0), StatusAgendamento.CONCLUIDO, "45.50");
        salvar(DIA.atTime(14, 0), DIA.atTime(15, 0), StatusAgendamento.AGENDADO, "300.00");

        BigDecimal receita = repository.somarReceita(StatusAgendamento.CONCLUIDO,
                DIA.withDayOfMonth(1).atStartOfDay(), DIA.withDayOfMonth(1).plusMonths(1).atStartOfDay());

        assertThat(receita).isEqualByComparingTo("125.50");
    }

    @Test
    @DisplayName("Receita é nula quando não há atendimentos concluídos")
    void receitaNulaSemAtendimentos() {
        BigDecimal receita = repository.somarReceita(StatusAgendamento.CONCLUIDO,
                DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay());

        assertThat(receita).isNull();
    }

    @Test
    @DisplayName("Lista os agendamentos de um profissional em um período, em ordem")
    void listaAgendamentosDoPeriodoEmOrdem() {
        salvar(DIA.atTime(14, 0), DIA.atTime(15, 0), StatusAgendamento.AGENDADO, "80.00");
        salvar(DIA.atTime(9, 0), DIA.atTime(10, 0), StatusAgendamento.AGENDADO, "80.00");
        salvar(DIA.plusDays(7).atTime(9, 0), DIA.plusDays(7).atTime(10, 0), StatusAgendamento.AGENDADO, "80.00");

        var agendamentos = repository.buscarNoPeriodoDoProfissional(profissional.getId(),
                DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay());

        assertThat(agendamentos).extracting(Agendamento::getInicio)
                .containsExactly(DIA.atTime(9, 0), DIA.atTime(14, 0));
    }

    @Test
    @DisplayName("Sabe se um cliente já tem histórico de agendamentos")
    void sabeSeClienteTemHistorico() {
        assertThat(repository.existsByClienteId(cliente.getId())).isFalse();

        salvar(DIA.atTime(9, 0), DIA.atTime(10, 0), StatusAgendamento.CONCLUIDO, "80.00");

        assertThat(repository.existsByClienteId(cliente.getId())).isTrue();
    }
}
