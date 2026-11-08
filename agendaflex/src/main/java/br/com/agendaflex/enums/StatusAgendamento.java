package br.com.agendaflex.enums;

import java.util.List;

/**
 * Ciclo de vida de um agendamento.
 *
 * AGENDADO -> CONFIRMADO -> CONCLUIDO
 *        \-> CANCELADO / FALTOU (a partir de AGENDADO ou CONFIRMADO)
 *
 * CONCLUIDO, CANCELADO e FALTOU são estados finais.
 */
public enum StatusAgendamento {

    AGENDADO("Agendado"),
    CONFIRMADO("Confirmado"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado"),
    FALTOU("Faltou");

    /** Status que ocupam a agenda do profissional. */
    public static final List<StatusAgendamento> ATIVOS = List.of(AGENDADO, CONFIRMADO);

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isAtivo() {
        return this == AGENDADO || this == CONFIRMADO;
    }

    public boolean podeTransitarPara(StatusAgendamento destino) {
        if (destino == null || destino == this) {
            return false;
        }
        return switch (this) {
            case AGENDADO -> true;
            case CONFIRMADO -> destino == CONCLUIDO || destino == CANCELADO || destino == FALTOU;
            case CONCLUIDO, CANCELADO, FALTOU -> false;
        };
    }
}
