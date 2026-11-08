package br.com.agendaflex.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    @DisplayName("Cliente novo começa ativo e sem faltas")
    void clienteNovoComecaAtivoESemFaltas() {
        Cliente cliente = new Cliente("Maria", "maria@email.com", "(47) 99999-0000");

        assertThat(cliente.isAtivo()).isTrue();
        assertThat(cliente.getFaltas()).isZero();
    }

    @Test
    @DisplayName("Registrar falta soma uma unidade ao contador")
    void registrarFaltaIncrementaContador() {
        Cliente cliente = new Cliente("Maria", "maria@email.com", null);

        cliente.registrarFalta();
        cliente.registrarFalta();

        assertThat(cliente.getFaltas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Fica bloqueado ao atingir o limite de faltas")
    void ficaBloqueadoAoAtingirOLimite() {
        Cliente cliente = new Cliente("Maria", "maria@email.com", null);
        cliente.setFaltas(2);
        assertThat(cliente.estaBloqueado(3)).isFalse();

        cliente.registrarFalta();

        assertThat(cliente.estaBloqueado(3)).isTrue();
    }

    @Test
    @DisplayName("Zerar faltas libera o cliente")
    void zerarFaltasLiberaOCliente() {
        Cliente cliente = new Cliente("Maria", "maria@email.com", null);
        cliente.setFaltas(5);
        assertThat(cliente.estaBloqueado(3)).isTrue();

        cliente.zerarFaltas();

        assertThat(cliente.getFaltas()).isZero();
        assertThat(cliente.estaBloqueado(3)).isFalse();
    }
}
