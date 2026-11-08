package br.com.agendaflex.util;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

public final class TextoUtil {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private TextoUtil() {
    }

    /** DayOfWeek.MONDAY -> "Segunda-feira". */
    public static String nomeDoDia(DayOfWeek dia) {
        String nome = dia.getDisplayName(TextStyle.FULL, PT_BR);
        return nome.substring(0, 1).toUpperCase(PT_BR) + nome.substring(1);
    }

    /** Remove espaços das pontas; texto vazio vira null. */
    public static String limparOuNulo(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
