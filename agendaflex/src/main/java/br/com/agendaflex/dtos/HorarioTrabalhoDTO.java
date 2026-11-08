package br.com.agendaflex.dtos;

import br.com.agendaflex.util.TextoUtil;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class HorarioTrabalhoDTO {

    private Long id;

    private Long profissionalId;

    @NotNull(message = "Escolha o dia da semana.")
    private DayOfWeek diaSemana;

    @NotNull(message = "Informe a hora inicial.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "Informe a hora final.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaFim;

    /** "Segunda-feira", para exibição. */
    public String getDiaSemanaTexto() {
        return diaSemana == null ? "" : TextoUtil.nomeDoDia(diaSemana);
    }
}
