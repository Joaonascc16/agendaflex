package br.com.agendaflex.dtos;

import br.com.agendaflex.enums.StatusAgendamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Agendamento pronto para exibição, já com as ações permitidas calculadas pelas regras. */
@Getter
@Setter
@NoArgsConstructor
public class AgendamentoResumoDTO {

    private Long id;

    private Long clienteId;
    private String clienteNome;

    private Long profissionalId;
    private String profissionalNome;

    private Long servicoId;
    private String servicoNome;

    private LocalDateTime inicio;
    private LocalDateTime fim;
    private StatusAgendamento status;
    private BigDecimal preco;
    private String observacoes;
    private String motivoCancelamento;
    private int vezesReagendado;

    // Ações liberadas neste momento
    private boolean podeConfirmar;
    private boolean podeCancelar;
    private boolean podeReagendar;
    private boolean podeFinalizar;
}
