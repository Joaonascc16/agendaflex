package br.com.agendaflex.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Item de lista de seleção: o valor enviado e o texto exibido. */
@Getter
@AllArgsConstructor
public class OpcaoDTO {

    private final String valor;
    private final String rotulo;
}
