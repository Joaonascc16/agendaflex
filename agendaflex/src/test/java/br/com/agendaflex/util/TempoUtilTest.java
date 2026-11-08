package br.com.agendaflex.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TempoUtilTest {

    @ParameterizedTest(name = "{0} minutos = {1}")
    @CsvSource({
            "1,1 minuto",
            "30,30 minutos",
            "45,45 minutos",
            "60,1 hora",
            "90,90 minutos",
            "120,2 horas"
    })
    void descreveMinutosEmTextoLegivel(int minutos, String esperado) {
        assertThat(TempoUtil.descreverMinutos(minutos)).isEqualTo(esperado);
    }
}
