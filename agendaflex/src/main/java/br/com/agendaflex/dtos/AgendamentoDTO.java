package br.com.agendaflex.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Dados enviados pelo formulário de novo agendamento. */
@Getter
@Setter
@NoArgsConstructor
public class AgendamentoDTO {

    @NotNull(message = "Escolha o cliente.")
    private Long clienteId;

    @NotNull(message = "Escolha o profissional.")
    private Long profissionalId;

    @NotNull(message = "Escolha o serviço.")
    private Long servicoId;

    @NotNull(message = "Escolha um horário.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime inicio;

    @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
    private String observacoes;

    public AgendamentoDTO(Long clienteId, Long profissionalId, Long servicoId, LocalDateTime inicio) {
        this.clienteId = clienteId;
        this.profissionalId = profissionalId;
        this.servicoId = servicoId;
        this.inicio = inicio;
    }
}
