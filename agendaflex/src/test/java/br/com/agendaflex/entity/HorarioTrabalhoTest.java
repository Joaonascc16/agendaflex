package br.com.agendaflex.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HorarioTrabalhoTest {

    private final HorarioTrabalho manha =
            new HorarioTrabalho(new Profissional("Ana", null), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

    @Test
    @DisplayName("Cobre um atendimento que cabe inteiro na janela")
    void cobreAtendimentoDentroDaJanela() {
        assertThat(manha.cobre(LocalTime.of(9, 0), LocalTime.of(10, 0))).isTrue();
    }

    @Test
    @DisplayName("Cobre um atendimento que começa na abertura e termina no fechamento")
    void cobreAtendimentoNosLimites() {
        assertThat(manha.cobre(LocalTime.of(8, 0), LocalTime.of(12, 0))).isTrue();
    }

    @Test
    @DisplayName("Não cobre um atendimento que termina depois do fechamento")
    void naoCobreQuandoTerminaDepoisDoFechamento() {
        assertThat(manha.cobre(LocalTime.of(11, 30), LocalTime.of(12, 30))).isFalse();
    }

    @Test
    @DisplayName("Não cobre um atendimento que começa antes da abertura")
    void naoCobreQuandoComecaAntesDaAbertura() {
        assertThat(manha.cobre(LocalTime.of(7, 30), LocalTime.of(8, 30))).isFalse();
    }

    @Test
    @DisplayName("Janelas que se cruzam se sobrepõem")
    void janelasQueSeCruzamSeSobrepoem() {
        assertThat(manha.sobrepoe(LocalTime.of(11, 0), LocalTime.of(13, 0))).isTrue();
    }

    @Test
    @DisplayName("Janelas que apenas se encostam não se sobrepõem")
    void janelasQueSoSeEncostamNaoSeSobrepoem() {
        assertThat(manha.sobrepoe(LocalTime.of(12, 0), LocalTime.of(18, 0))).isFalse();
        assertThat(manha.sobrepoe(LocalTime.of(6, 0), LocalTime.of(8, 0))).isFalse();
    }
}
