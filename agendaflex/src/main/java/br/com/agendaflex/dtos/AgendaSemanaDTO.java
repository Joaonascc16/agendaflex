package br.com.agendaflex.dtos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgendaSemanaDTO {

    private Long profissionalId;
    private LocalDate inicioSemana;
    private LocalDate fimSemana;
    private LocalDate semanaAnterior;
    private LocalDate proximaSemana;

    /** Rótulos da coluna de horas (07:00, 08:00...). */
    private List<String> horas = new ArrayList<>();
    private int alturaPx;

    private List<DiaAgendaDTO> dias = new ArrayList<>();
}
