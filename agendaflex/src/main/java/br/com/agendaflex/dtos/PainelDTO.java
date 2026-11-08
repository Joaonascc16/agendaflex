package br.com.agendaflex.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PainelDTO {

    private LocalDate hoje;
    private long atendimentosHoje;
    private long aguardandoConfirmacao;
    private long proximosSeteDias;
    private long faltasNoMes;
    private BigDecimal receitaDoMes = BigDecimal.ZERO;
    private List<AgendamentoResumoDTO> proximos = new ArrayList<>();
}
