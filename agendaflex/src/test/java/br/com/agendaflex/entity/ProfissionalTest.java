package br.com.agendaflex.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfissionalTest {

    private Servico corte;
    private Servico barba;
    private Profissional profissional;

    @BeforeEach
    void preparar() {
        corte = new Servico("Corte", null, 45, new BigDecimal("60.00"));
        corte.setId(1L);
        barba = new Servico("Barba", null, 30, new BigDecimal("40.00"));
        barba.setId(2L);

        profissional = new Profissional("Bruno", "Barbeiro");
        profissional.getServicos().add(corte);
    }

    @Test
    @DisplayName("Atende o serviço quando está vinculado a ele")
    void atendeServicoVinculado() {
        assertThat(profissional.atendeServico(corte)).isTrue();
    }

    @Test
    @DisplayName("Não atende serviço que não está vinculado")
    void naoAtendeServicoNaoVinculado() {
        assertThat(profissional.atendeServico(barba)).isFalse();
    }

    @Test
    @DisplayName("Reconhece o serviço pelo id, mesmo vindo de outra instância")
    void reconhecePeloId() {
        Servico mesmoCorteEmOutraInstancia = new Servico("Corte", null, 45, new BigDecimal("60.00"));
        mesmoCorteEmOutraInstancia.setId(1L);

        assertThat(profissional.atendeServico(mesmoCorteEmOutraInstancia)).isTrue();
    }

    @Test
    @DisplayName("Serviço nulo nunca é atendido")
    void servicoNuloNaoEAtendido() {
        assertThat(profissional.atendeServico(null)).isFalse();
    }

    @Test
    @DisplayName("Profissional novo começa ativo")
    void profissionalNovoComecaAtivo() {
        assertThat(profissional.isAtivo()).isTrue();
    }
}
