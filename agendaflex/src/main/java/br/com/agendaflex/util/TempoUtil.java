package br.com.agendaflex.util;

public final class TempoUtil {

    private TempoUtil() {
    }

    /** 120 -> "2 horas", 60 -> "1 hora", 45 -> "45 minutos". */
    public static String descreverMinutos(int minutos) {
        if (minutos >= 60 && minutos % 60 == 0) {
            int horas = minutos / 60;
            return horas + (horas == 1 ? " hora" : " horas");
        }
        return minutos + (minutos == 1 ? " minuto" : " minutos");
    }
}
