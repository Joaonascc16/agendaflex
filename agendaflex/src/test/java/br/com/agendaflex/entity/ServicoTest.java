package br.com.agendaflex.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServicoTest {

    @Test
    @DisplayName("O fim do atendimento é o início somado à duração")
    void calculaFimSomandoDuracao() {
        Servico servico = new Servico("Corte", null, 45, new BigDecimal("60.00"));

        LocalDateTime fim = servico.calcularFim(LocalDateTime.of(2026, 11, 10, 9, 30));

        assertThat(fim).isEqualTo(LocalDateTime.of(2026, 11, 10, 10, 15));
    }

    @Test
    @DisplayName("Serviço de duas horas atravessa a virada de hora corretamente")
    void calculaFimDeServicoLongo() {
        Servico servico = new Servico("Coloração", "Completa", 120, new BigDecimal("180.00"));

        LocalDateTime fim = servico.calcularFim(LocalDateTime.of(2026, 11, 10, 11, 0));

        assertThat(fim).isEqualTo(LocalDateTime.of(2026, 11, 10, 13, 0));
    }

    @Test
    @DisplayName("Serviço novo começa ativo")
    void servicoNovoComecaAtivo() {
        Servico servico = new Servico("Barba", null, 30, new BigDecimal("40.00"));

        assertThat(servico.isAtivo()).isTrue();
    }
}
