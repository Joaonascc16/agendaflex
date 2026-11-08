package br.com.agendaflex.dtos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;

/** Um horário livre que pode ser escolhido pelo usuário. */
@Getter
public class SlotDTO {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter VALOR = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final LocalDateTime inicio;
    private final LocalDateTime fim;

    public SlotDTO(LocalDateTime inicio, LocalDateTime fim) {
        this.inicio = inicio;
        this.fim = fim;
    }

    /** Texto exibido no botão: 09:30 */
    public String getHoraTexto() {
        return inicio.format(HORA);
    }

    /** Valor enviado pelo formulário: 2026-11-10T09:30 */
    public String getValor() {
        return inicio.format(VALOR);
    }
}
