package br.com.agendaflex;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.agendaflex.config.RegrasAgendamentoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "agendaflex.dados-demo=false")
class AgendaFlexApplicationTests {

    @Autowired
    private RegrasAgendamentoProperties regras;

    @Test
    void contextoSobeEAsRegrasSaoLidasDoApplicationProperties() {
        assertThat(regras.getAntecedenciaMinimaMinutos()).isEqualTo(120);
        assertThat(regras.getLimiteFaltasParaBloqueio()).isEqualTo(3);
        assertThat(regras.getPassoSlotMinutos()).isEqualTo(30);
    }
}
