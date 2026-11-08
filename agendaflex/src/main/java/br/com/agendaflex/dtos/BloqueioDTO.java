package br.com.agendaflex.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class BloqueioDTO {

    private Long id;

    private Long profissionalId;

    @NotNull(message = "Informe quando o bloqueio começa.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime inicio;

    @NotNull(message = "Informe quando o bloqueio termina.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fim;

    @NotBlank(message = "Informe o motivo.")
    @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres.")
    private String motivo;
}
