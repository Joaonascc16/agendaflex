package br.com.agendaflex.dtos;

import br.com.agendaflex.enums.StatusAgendamento;
import lombok.Getter;
import lombok.Setter;

/** Um retângulo da agenda semanal: um agendamento ou um bloqueio do profissional. */
@Getter
@Setter
public class BlocoAgendaDTO {

    /** Id do agendamento (nulo para bloqueios). */
    private Long id;

    private String titulo;
    private String subtitulo;
    private String horario;
    private boolean bloqueio;
    private StatusAgendamento status;

    /** Distância do topo da grade, em pixels (1 minuto = 1 pixel). */
    private int topoPx;
    private int alturaPx;
}
