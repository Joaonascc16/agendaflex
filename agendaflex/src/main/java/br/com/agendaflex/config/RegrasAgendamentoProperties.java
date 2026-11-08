package br.com.agendaflex.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Regras de negócio configuráveis em application.properties (prefixo agendaflex.regras).
 * Alterar um valor muda o comportamento do sistema sem mexer no código.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "agendaflex.regras")
public class RegrasAgendamentoProperties {

    /** Antecedência mínima para criar um agendamento. */
    private int antecedenciaMinimaMinutos = 120;

    /** Até quantos dias à frente é possível agendar. */
    private int antecedenciaMaximaDias = 60;

    /** Antecedência mínima para cancelar ou reagendar. */
    private int antecedenciaMinimaCancelamentoMinutos = 120;

    private int maximoReagendamentos = 2;

    private int maximoAgendamentosAtivosPorCliente = 3;

    private int maximoAgendamentosPorClientePorDia = 2;

    /** Ao atingir esse número de faltas, o cliente fica bloqueado para novos agendamentos. */
    private int limiteFaltasParaBloqueio = 3;

    /** Os horários são oferecidos de N em N minutos (ex.: 30 -> 09:00, 09:30, 10:00...). */
    private int passoSlotMinutos = 30;

    /** Janela exibida na grade da agenda semanal. */
    private int horaInicialAgenda = 7;

    private int horaFinalAgenda = 21;
}
