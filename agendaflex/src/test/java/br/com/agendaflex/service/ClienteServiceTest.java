package br.com.agendaflex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.agendaflex.config.RegrasAgendamentoProperties;
import br.com.agendaflex.dtos.ClienteDTO;
import br.com.agendaflex.entity.Cliente;
import br.com.agendaflex.entity.Estabelecimento;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.ClienteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private TenantContext tenantContext;

    private ClienteService clienteService;

    @BeforeEach
    void preparar() {
        Estabelecimento estabelecimento = new Estabelecimento("Principal", "principal", null, null);
        estabelecimento.setId(1L);
        when(tenantContext.obterEstabelecimentoId()).thenReturn(1L);
        when(tenantContext.obterEstabelecimento()).thenReturn(estabelecimento);
        clienteService = new ClienteService(
                clienteRepository,
                agendamentoRepository,
                new RegrasAgendamentoProperties(),
                tenantContext
        );
    }

    @Test
    @DisplayName("Normaliza o e-mail antes de salvar um cliente")
    void normalizaEmailAoCadastrar() {
        ClienteDTO dto = novoCliente();
        dto.setEmail("  MARIA@EMAIL.COM  ");

        when(clienteRepository.findByEmailIgnoreCaseAndEstabelecimentoId("maria@email.com", 1L))
                .thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteDTO salvo = clienteService.salvar(dto);

        assertThat(salvo.getEmail()).isEqualTo("maria@email.com");
        assertThat(salvo.getNome()).isEqualTo("Maria Silva");
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Não permite cadastrar e-mail que já pertence a outro cliente")
    void rejeitaEmailDuplicado() {
        Cliente existente = new Cliente("Outra pessoa", "maria@email.com", null);
        existente.setId(10L);
        when(clienteRepository.findByEmailIgnoreCaseAndEstabelecimentoId("maria@email.com", 1L))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> clienteService.salvar(novoCliente()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe um cliente");

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Não exclui cliente que possui histórico de agendamentos")
    void impedeExcluirClienteComHistorico() {
        Cliente cliente = new Cliente("Maria Silva", "maria@email.com", null);
        cliente.setId(1L);
        when(clienteRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.existsByClienteIdAndEstabelecimentoId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.excluir(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("histórico de agendamentos");

        verify(clienteRepository, never()).delete(any(Cliente.class));
    }

    @Test
    @DisplayName("Permite excluir cliente sem histórico")
    void excluiClienteSemHistorico() {
        Cliente cliente = new Cliente("Maria Silva", "maria@email.com", null);
        cliente.setId(1L);
        when(clienteRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.existsByClienteIdAndEstabelecimentoId(1L, 1L)).thenReturn(false);

        clienteService.excluir(1L);

        verify(clienteRepository).delete(cliente);
    }

    @Test
    @DisplayName("Zera as faltas do cliente")
    void zeraFaltasDoCliente() {
        Cliente cliente = new Cliente("Maria Silva", "maria@email.com", null);
        cliente.setId(1L);
        cliente.registrarFalta();
        cliente.registrarFalta();
        when(clienteRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(cliente));

        clienteService.zerarFaltas(1L);

        assertThat(cliente.getFaltas()).isZero();
    }

    private ClienteDTO novoCliente() {
        ClienteDTO dto = new ClienteDTO();
        dto.setNome("Maria Silva");
        dto.setEmail("maria@email.com");
        dto.setTelefone("47999999999");
        dto.setAtivo(true);
        return dto;
    }
}
