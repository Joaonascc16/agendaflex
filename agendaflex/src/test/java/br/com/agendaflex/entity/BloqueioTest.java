package br.com.agendaflex.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BloqueioTest {

    private final Bloqueio ferias = new Bloqueio(
            new Profissional("Ana", null),
            LocalDateTime.of(2026, 11, 16, 0, 0),
            LocalDateTime.of(2026, 11, 20, 23, 59),
            "Férias");

    @Test
    @DisplayName("Atendimento dentro do período bloqueado conflita")
    void atendimentoDentroDoPeriodoConflita() {
        assertThat(ferias.sobrepoe(
                LocalDateTime.of(2026, 11, 17, 10, 0), LocalDateTime.of(2026, 11, 17, 11, 0))).isTrue();
    }

    @Test
    @DisplayName("Atendimento que invade o início do bloqueio conflita")
    void atendimentoQueInvadeOInicioConflita() {
        assertThat(ferias.sobrepoe(
                LocalDateTime.of(2026, 11, 15, 23, 30), LocalDateTime.of(2026, 11, 16, 0, 30))).isTrue();
    }

    @Test
    @DisplayName("Atendimento que termina exatamente quando o bloqueio começa não conflita")
    void atendimentoQueTerminaNoInicioNaoConflita() {
        assertThat(ferias.sobrepoe(
                LocalDateTime.of(2026, 11, 15, 22, 0), LocalDateTime.of(2026, 11, 16, 0, 0))).isFalse();
    }

    @Test
    @DisplayName("Atendimento depois do bloqueio não conflita")
    void atendimentoDepoisDoBloqueioNaoConflita() {
        assertThat(ferias.sobrepoe(
                LocalDateTime.of(2026, 11, 23, 9, 0), LocalDateTime.of(2026, 11, 23, 10, 0))).isFalse();
    }
}
