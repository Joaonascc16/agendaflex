package br.com.agendaflex.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfissionalDTO {

    private Long id;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    @Size(max = 100, message = "A especialidade deve ter no máximo 100 caracteres.")
    private String especialidade;

    private boolean ativo = true;

    /** Serviços que o profissional realiza (ids marcados no formulário). */
    private List<Long> servicosIds = new ArrayList<>();

    // Somente leitura
    private List<String> servicosNomes = new ArrayList<>();
}
