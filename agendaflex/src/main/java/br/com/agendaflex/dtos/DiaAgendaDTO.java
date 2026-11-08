package br.com.agendaflex.dtos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaAgendaDTO {

    private LocalDate data;
    private boolean hoje;
    private List<BlocoAgendaDTO> blocos = new ArrayList<>();
}
