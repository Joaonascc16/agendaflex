package br.com.agendaflex.entity;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.agendaflex.enums.StatusAgendamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatusAgendamentoTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "AGENDADO,CONFIRMADO,true",
            "AGENDADO,CANCELADO,true",
            "AGENDADO,CONCLUIDO,true",
            "AGENDADO,FALTOU,true",
            "AGENDADO,AGENDADO,false",
            "CONFIRMADO,CONCLUIDO,true",
            "CONFIRMADO,CANCELADO,true",
            "CONFIRMADO,FALTOU,true",
            "CONFIRMADO,AGENDADO,false",
            "CONFIRMADO,CONFIRMADO,false",
            "CONCLUIDO,CANCELADO,false",
            "CANCELADO,AGENDADO,false",
            "FALTOU,CONCLUIDO,false"
    })
    @DisplayName("Transições permitidas entre status")
    void transicoes(StatusAgendamento origem, StatusAgendamento destino, boolean esperado) {
        assertThat(origem.podeTransitarPara(destino)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Só Agendado e Confirmado ocupam a agenda")
    void apenasAgendadoEConfirmadoSaoAtivos() {
        assertThat(StatusAgendamento.ATIVOS)
                .containsExactly(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO);
        assertThat(StatusAgendamento.CONCLUIDO.isAtivo()).isFalse();
        assertThat(StatusAgendamento.CANCELADO.isAtivo()).isFalse();
        assertThat(StatusAgendamento.FALTOU.isAtivo()).isFalse();
    }
}
