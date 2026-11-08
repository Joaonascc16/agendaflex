package br.com.agendaflex.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.exception.RegraNegocioException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgendamentoTest {

    /** Segunda-feira, 09/11/2026, 10:00. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 9, 10, 0);
    private static final int ANTECEDENCIA_MINUTOS = 120;
    private static final int MAXIMO_REAGENDAMENTOS = 2;

    private Agendamento agendamentoEm(LocalDateTime inicio, int duracaoMinutos) {
        Agendamento agendamento = new Agendamento();
        agendamento.setInicio(inicio);
        agendamento.setFim(inicio.plusMinutes(duracaoMinutos));
        return agendamento;
    }

    // ------------------------------------------------------------------
    // Status
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Agendamento novo começa como Agendado e ocupa a agenda")
    void agendamentoNovoComecaAgendado() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(agendamento.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Confirmar muda de Agendado para Confirmado")
    void confirmarMudaOStatus() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);

        agendamento.confirmar();

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("Não é possível confirmar duas vezes")
    void naoConfirmaDuasVezes() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);
        agendamento.confirmar();

        assertThatThrownBy(agendamento::confirmar)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não é possível mudar");
    }

    // ------------------------------------------------------------------
    // Concluir e faltar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Não conclui um atendimento que ainda não começou")
    void naoConcluiAntesDoInicio() {
        Agendamento agendamento = agendamentoEm(AGORA.plusHours(5), 60);

        assertThatThrownBy(() -> agendamento.concluir(AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("depois do horário de início");
    }

    @Test
    @DisplayName("Conclui um atendimento depois do início")
    void concluiDepoisDoInicio() {
        Agendamento agendamento = agendamentoEm(AGORA.minusHours(1), 60);

        agendamento.concluir(AGORA);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
        assertThat(agendamento.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Registra falta depois do início")
    void registraFaltaDepoisDoInicio() {
        Agendamento agendamento = agendamentoEm(AGORA.minusHours(1), 60);

        agendamento.registrarFalta(AGORA);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.FALTOU);
    }

    @Test
    @DisplayName("Não registra falta de um atendimento futuro")
    void naoRegistraFaltaAntesDoInicio() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);

        assertThatThrownBy(() -> agendamento.registrarFalta(AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("depois do horário de início");
    }

    // ------------------------------------------------------------------
    // Cancelamento
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Cancelar exige motivo")
    void cancelarExigeMotivo() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);

        assertThatThrownBy(() -> agendamento.cancelar("  ", AGORA, ANTECEDENCIA_MINUTOS))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    @DisplayName("Cancela com antecedência suficiente e guarda o motivo")
    void cancelaComAntecedenciaSuficiente() {
        Agendamento agendamento = agendamentoEm(AGORA.plusHours(5), 60);

        agendamento.cancelar("  Imprevisto no trabalho ", AGORA, ANTECEDENCIA_MINUTOS);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(agendamento.getMotivoCancelamento()).isEqualTo("Imprevisto no trabalho");
        assertThat(agendamento.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Cancela quando falta exatamente a antecedência mínima")
    void cancelaNaFronteiraDaAntecedencia() {
        Agendamento agendamento = agendamentoEm(AGORA.plusMinutes(ANTECEDENCIA_MINUTOS), 60);

        agendamento.cancelar("Mudança de planos", AGORA, ANTECEDENCIA_MINUTOS);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    @DisplayName("Não cancela fora da antecedência mínima")
    void naoCancelaForaDaAntecedencia() {
        Agendamento agendamento = agendamentoEm(AGORA.plusMinutes(60), 60);

        assertThatThrownBy(() -> agendamento.cancelar("Mudança de planos", AGORA, ANTECEDENCIA_MINUTOS))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("antecedência mínima de 2 horas");
    }

    @Test
    @DisplayName("Não cancela um atendimento já concluído")
    void naoCancelaAtendimentoConcluido() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);

        assertThatThrownBy(() -> agendamento.cancelar("Tarde demais", AGORA, ANTECEDENCIA_MINUTOS))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode ser cancelado");
    }

    // ------------------------------------------------------------------
    // Reagendamento
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Reagendar troca o horário, conta o reagendamento e volta para Agendado")
    void reagendarTrocaHorarioEVoltaParaAgendado() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);
        agendamento.confirmar();
        LocalDateTime novoInicio = AGORA.plusDays(2);

        agendamento.reagendar(novoInicio, novoInicio.plusMinutes(60), AGORA, ANTECEDENCIA_MINUTOS, MAXIMO_REAGENDAMENTOS);

        assertThat(agendamento.getInicio()).isEqualTo(novoInicio);
        assertThat(agendamento.getFim()).isEqualTo(novoInicio.plusMinutes(60));
        assertThat(agendamento.getVezesReagendado()).isEqualTo(1);
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    @DisplayName("Não reagenda além do limite de reagendamentos")
    void naoReagendaAlemDoLimite() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);
        agendamento.setVezesReagendado(MAXIMO_REAGENDAMENTOS);

        assertThatThrownBy(() -> agendamento.reagendar(
                AGORA.plusDays(2), AGORA.plusDays(2).plusMinutes(60), AGORA, ANTECEDENCIA_MINUTOS, MAXIMO_REAGENDAMENTOS))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("limite");
    }

    @Test
    @DisplayName("Não reagenda fora da antecedência mínima")
    void naoReagendaForaDaAntecedencia() {
        Agendamento agendamento = agendamentoEm(AGORA.plusMinutes(30), 60);

        assertThatThrownBy(() -> agendamento.validarPodeReagendar(AGORA, ANTECEDENCIA_MINUTOS, MAXIMO_REAGENDAMENTOS))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("antecedência mínima");
    }

    @Test
    @DisplayName("Não reagenda um agendamento cancelado")
    void naoReagendaAgendamentoCancelado() {
        Agendamento agendamento = agendamentoEm(AGORA.plusDays(1), 60);
        agendamento.cancelar("Desistência", AGORA, ANTECEDENCIA_MINUTOS);

        assertThatThrownBy(() -> agendamento.validarPodeReagendar(AGORA, ANTECEDENCIA_MINUTOS, MAXIMO_REAGENDAMENTOS))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ativos");
    }

    // ------------------------------------------------------------------
    // Ações permitidas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Indica quando cancelar e reagendar ainda são permitidos")
    void indicaAcoesPermitidas() {
        Agendamento distante = agendamentoEm(AGORA.plusDays(1), 60);
        Agendamento iminente = agendamentoEm(AGORA.plusMinutes(30), 60);

        assertThat(distante.podeCancelar(AGORA, ANTECEDENCIA_MINUTOS)).isTrue();
        assertThat(distante.podeReagendar(AGORA, ANTECEDENCIA_MINUTOS, MAXIMO_REAGENDAMENTOS)).isTrue();
        assertThat(iminente.podeCancelar(AGORA, ANTECEDENCIA_MINUTOS)).isFalse();
        assertThat(iminente.podeReagendar(AGORA, ANTECEDENCIA_MINUTOS, MAXIMO_REAGENDAMENTOS)).isFalse();
    }

    // ------------------------------------------------------------------
    // Sobreposição de horários
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Detecta sobreposição parcial e total")
    void detectaSobreposicao() {
        Agendamento agendamento = agendamentoEm(LocalDateTime.of(2026, 11, 10, 10, 0), 60);

        assertThat(agendamento.sobrepoe(
                LocalDateTime.of(2026, 11, 10, 10, 30), LocalDateTime.of(2026, 11, 10, 11, 30))).isTrue();
        assertThat(agendamento.sobrepoe(
                LocalDateTime.of(2026, 11, 10, 9, 0), LocalDateTime.of(2026, 11, 10, 12, 0))).isTrue();
    }

    @Test
    @DisplayName("Atendimentos colados, um logo após o outro, não se sobrepõem")
    void atendimentosColadosNaoSeSobrepoem() {
        Agendamento agendamento = agendamentoEm(LocalDateTime.of(2026, 11, 10, 10, 0), 60);

        assertThat(agendamento.sobrepoe(
                LocalDateTime.of(2026, 11, 10, 11, 0), LocalDateTime.of(2026, 11, 10, 12, 0))).isFalse();
        assertThat(agendamento.sobrepoe(
                LocalDateTime.of(2026, 11, 10, 9, 0), LocalDateTime.of(2026, 11, 10, 10, 0))).isFalse();
    }
}
