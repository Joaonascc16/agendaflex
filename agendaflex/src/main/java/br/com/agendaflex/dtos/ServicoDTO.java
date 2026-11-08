package br.com.agendaflex.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServicoDTO {

    private Long id;

    @NotBlank(message = "Informe o nome do serviço.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    @Size(max = 400, message = "A descrição deve ter no máximo 400 caracteres.")
    private String descricao;

    @NotNull(message = "Informe a duração.")
    @Min(value = 5, message = "A duração mínima é de 5 minutos.")
    @Max(value = 480, message = "A duração máxima é de 480 minutos.")
    private Integer duracaoMinutos;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
    private BigDecimal preco;

    private boolean ativo = true;
}
