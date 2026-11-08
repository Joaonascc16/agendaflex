package br.com.agendaflex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import br.com.agendaflex.config.RegrasAgendamentoProperties;
import br.com.agendaflex.dtos.AgendamentoDTO;
import br.com.agendaflex.dtos.AgendamentoResumoDTO;
import br.com.agendaflex.dtos.SlotDTO;
import br.com.agendaflex.entity.Agendamento;
import br.com.agendaflex.entity.Bloqueio;
import br.com.agendaflex.entity.Cliente;
import br.com.agendaflex.entity.HorarioTrabalho;
import br.com.agendaflex.entity.Profissional;
import br.com.agendaflex.entity.Servico;
import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.exception.RecursoNaoEncontradoException;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.BloqueioRepository;
import br.com.agendaflex.repository.ClienteRepository;
import br.com.agendaflex.repository.HorarioTrabalhoRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import br.com.agendaflex.repository.ServicoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Testa as regras de negócio do agendamento sem banco de dados:
 * os repositórios são simulados (mocks) e o relógio é fixo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendamentoServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    /** Segunda-feira, 09/11/2026, 10:00. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 9, 10, 0);
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 9);
    private static final LocalDate TERCA = LocalDate.of(2026, 11, 10);
    private static final LocalDate QUARTA = LocalDate.of(2026, 11, 11);

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private HorarioTrabalhoRepository horarioTrabalhoRepository;
    @Mock private BloqueioRepository bloqueioRepository;

    private AgendamentoService service;
    private Cliente cliente;
    private Profissional profissional;
    private Servico servico;

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA);
        RegrasAgendamentoProperties regras = new RegrasAgendamentoProperties();
        service = new AgendamentoService(agendamentoRepository, clienteRepository, profissionalRepository,
                servicoRepository, horarioTrabalhoRepository, bloqueioRepository, regras, relogio);

        servico = new Servico("Corte", null, 60, new BigDecimal("80.00"));
        servico.setId(10L);

        profissional = new Profissional("Ana", "Cabeleireira");
        profissional.setId(2L);
        profissional.getServicos().add(servico);

        cliente = new Cliente("Maria", "maria@email.com", null);
        cliente.setId(1L);

        lenient().when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(profissionalRepository.findById(2L)).thenReturn(Optional.of(profissional));
        lenient().when(servicoRepository.findById(10L)).thenReturn(Optional.of(servico));
        lenient().when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        // Padrão: a profissional atende de segunda a quarta, das 08:00 às 18:00
        for (DayOfWeek dia : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)) {
            lenient().when(horarioTrabalhoRepository.findByProfissionalIdAndDiaSemanaOrderByHoraInicio(2L, dia))
                    .thenReturn(List.of(new HorarioTrabalho(profissional, dia, LocalTime.of(8, 0), LocalTime.of(18, 0))));
        }
    }

    private AgendamentoDTO pedido(LocalDateTime inicio) {
        return new AgendamentoDTO(1L, 2L, 10L, inicio);
    }

    private Agendamento agendamentoExistente(LocalDateTime inicio, StatusAgendamento status) {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(100L);
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setInicio(inicio);
        agendamento.setFim(servico.calcularFim(inicio));
        agendamento.setStatus(status);
        agendamento.setPrecoCobrado(servico.getPreco());
        lenient().when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamento));
        return agendamento;
    }

    // ==================================================================
    // Criação
    // ==================================================================

    @Nested
    @DisplayName("Criar agendamento")
    class Criar {

        @Test
        @DisplayName("Cria quando todas as regras são atendidas")
        void criaAgendamentoValido() {
            AgendamentoResumoDTO criado = service.criar(pedido(TERCA.atTime(14, 0)));

            assertThat(criado.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
            assertThat(criado.getInicio()).isEqualTo(TERCA.atTime(14, 0));
            assertThat(criado.getFim()).isEqualTo(TERCA.atTime(15, 0));
            assertThat(criado.getPreco()).isEqualByComparingTo("80.00");
            assertThat(criado.getClienteNome()).isEqualTo("Maria");
            assertThat(criado.isPodeConfirmar()).isTrue();
        }

        @Test
        @DisplayName("Rejeita horário que já passou")
        void rejeitaHorarioNoPassado() {
            assertThatThrownBy(() -> service.criar(pedido(AGORA.minusHours(1))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("já passou");
        }

        @Test
        @DisplayName("Rejeita horário sem a antecedência mínima")
        void rejeitaSemAntecedenciaMinima() {
            assertThatThrownBy(() -> service.criar(pedido(SEGUNDA.atTime(10, 30))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("antecedência mínima de 2 horas");
        }

        @Test
        @DisplayName("Rejeita horário além da antecedência máxima")
        void rejeitaAlemDaAntecedenciaMaxima() {
            assertThatThrownBy(() -> service.criar(pedido(AGORA.plusDays(61))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("até 60 dias");
        }

        @Test
        @DisplayName("Rejeita horário fora da grade de 30 em 30 minutos")
        void rejeitaHorarioForaDaGrade() {
            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 15))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("30 em 30 minutos");
        }

        @Test
        @DisplayName("Rejeita atendimento que termina depois do expediente")
        void rejeitaForaDoExpediente() {
            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(17, 30))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não atende nesse dia e horário");
        }

        @Test
        @DisplayName("Rejeita dia em que o profissional não trabalha")
        void rejeitaDiaSemExpediente() {
            assertThatThrownBy(() -> service.criar(pedido(QUARTA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não atende nesse dia e horário");
        }

        @Test
        @DisplayName("Rejeita período bloqueado e informa o motivo")
        void rejeitaPeriodoBloqueado() {
            Bloqueio ferias = new Bloqueio(profissional, TERCA.atStartOfDay(), TERCA.plusDays(3).atStartOfDay(), "Férias");
            when(bloqueioRepository.buscarSobrepostos(eq(2L), any(), any())).thenReturn(List.of(ferias));

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Férias");
        }

        @Test
        @DisplayName("Rejeita horário já ocupado na agenda do profissional")
        void rejeitaConflitoComOutroAgendamentoDoProfissional() {
            when(agendamentoRepository.buscarConflitosDoProfissional(eq(2L), any(), any(), any(), eq(0L)))
                    .thenReturn(List.of(new Agendamento()));

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("já está ocupado");
        }

        @Test
        @DisplayName("Rejeita quando o cliente já tem outro atendimento no mesmo horário")
        void rejeitaConflitoComOutroAgendamentoDoCliente() {
            when(agendamentoRepository.buscarConflitosDoCliente(eq(1L), any(), any(), any(), eq(0L)))
                    .thenReturn(List.of(new Agendamento()));

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("cliente já tem outro agendamento");
        }

        @Test
        @DisplayName("Rejeita cliente bloqueado por excesso de faltas")
        void rejeitaClienteBloqueadoPorFaltas() {
            cliente.setFaltas(3);

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("bloqueado");
        }

        @Test
        @DisplayName("Rejeita cliente inativo")
        void rejeitaClienteInativo() {
            cliente.setAtivo(false);

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("Rejeita serviço inativo")
        void rejeitaServicoInativo() {
            servico.setAtivo(false);

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("Rejeita profissional que não realiza o serviço")
        void rejeitaProfissionalQueNaoRealizaOServico() {
            profissional.getServicos().clear();

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não realiza o serviço");
        }

        @Test
        @DisplayName("Rejeita quando o cliente já tem o máximo de agendamentos ativos")
        void rejeitaLimiteDeAgendamentosAtivos() {
            when(agendamentoRepository.countByClienteIdAndStatusInAndInicioAfter(eq(1L), any(), any()))
                    .thenReturn(3L);

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("agendamentos ativos");
        }

        @Test
        @DisplayName("Rejeita quando o cliente atinge o limite diário")
        void rejeitaLimiteDiario() {
            when(agendamentoRepository.contarDoClienteNoPeriodo(eq(1L), any(), any(), any())).thenReturn(2L);

            assertThatThrownBy(() -> service.criar(pedido(TERCA.atTime(14, 0))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("limite diário");
        }

        @Test
        @DisplayName("Falha com 'não encontrado' quando o cliente não existe")
        void falhaQuandoClienteNaoExiste() {
            AgendamentoDTO pedido = new AgendamentoDTO(99L, 2L, 10L, TERCA.atTime(14, 0));

            assertThatThrownBy(() -> service.criar(pedido))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    // ==================================================================
    // Horários disponíveis
    // ==================================================================

    @Nested
    @DisplayName("Horários disponíveis")
    class HorariosDisponiveis {

        private void expedienteDe9as12() {
            when(horarioTrabalhoRepository.findByProfissionalIdAndDiaSemanaOrderByHoraInicio(2L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(new HorarioTrabalho(profissional, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
        }

        private List<String> horas(List<SlotDTO> slots) {
            return slots.stream().map(SlotDTO::getHoraTexto).toList();
        }

        @Test
        @DisplayName("Oferece horários de 30 em 30 minutos que cabem no expediente")
        void ofereceTodosOsHorariosQueCabem() {
            expedienteDe9as12();

            List<SlotDTO> slots = service.horariosDisponiveis(2L, 10L, TERCA);

            // Serviço de 60 minutos: o último início possível é 11:00
            assertThat(horas(slots)).containsExactly("09:00", "09:30", "10:00", "10:30", "11:00");
        }

        @Test
        @DisplayName("Remove os horários que colidem com um agendamento existente")
        void removeHorariosOcupados() {
            expedienteDe9as12();
            Agendamento existente = new Agendamento();
            existente.setInicio(TERCA.atTime(10, 0));
            existente.setFim(TERCA.atTime(11, 0));
            when(agendamentoRepository.buscarConflitosDoProfissional(eq(2L), any(), any(), any(), eq(0L)))
                    .thenReturn(List.of(existente));

            List<SlotDTO> slots = service.horariosDisponiveis(2L, 10L, TERCA);

            assertThat(horas(slots)).containsExactly("09:00", "11:00");
        }

        @Test
        @DisplayName("Remove os horários que colidem com um bloqueio")
        void removeHorariosBloqueados() {
            expedienteDe9as12();
            Bloqueio consulta = new Bloqueio(profissional, TERCA.atTime(9, 0), TERCA.atTime(10, 30), "Consulta médica");
            when(bloqueioRepository.buscarSobrepostos(eq(2L), any(), any())).thenReturn(List.of(consulta));

            List<SlotDTO> slots = service.horariosDisponiveis(2L, 10L, TERCA);

            assertThat(horas(slots)).containsExactly("10:30", "11:00");
        }

        @Test
        @DisplayName("No dia de hoje, respeita a antecedência mínima")
        void respeitaAntecedenciaMinimaNoDiaDeHoje() {
            List<SlotDTO> slots = service.horariosDisponiveis(2L, 10L, SEGUNDA);

            // São 10:00 e a antecedência mínima é de 2 horas: o primeiro horário é 12:00
            assertThat(horas(slots)).first().isEqualTo("12:00");
            assertThat(horas(slots)).doesNotContain("10:00", "11:30");
        }

        @Test
        @DisplayName("Dia sem expediente não tem horários")
        void diaSemExpedienteNaoTemHorarios() {
            assertThat(service.horariosDisponiveis(2L, 10L, QUARTA)).isEmpty();
        }

        @Test
        @DisplayName("Profissional que não realiza o serviço não tem horários")
        void profissionalSemOServicoNaoTemHorarios() {
            profissional.getServicos().clear();

            assertThat(service.horariosDisponiveis(2L, 10L, TERCA)).isEmpty();
        }

        @Test
        @DisplayName("O valor do horário é aceito pelo formulário de agendamento")
        void valorDoSlotUsaFormatoDoFormulario() {
            expedienteDe9as12();

            SlotDTO primeiro = service.horariosDisponiveis(2L, 10L, TERCA).get(0);

            assertThat(primeiro.getValor()).isEqualTo("2026-11-10T09:00");
        }
    }

    // ==================================================================
    // Ações sobre agendamentos existentes
    // ==================================================================

    @Nested
    @DisplayName("Ações sobre um agendamento")
    class Acoes {

        @Test
        @DisplayName("Indica quais ações estão liberadas")
        void indicaAcoesLiberadas() {
            agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.AGENDADO);

            AgendamentoResumoDTO resumo = service.buscar(100L);

            assertThat(resumo.isPodeConfirmar()).isTrue();
            assertThat(resumo.isPodeCancelar()).isTrue();
            assertThat(resumo.isPodeReagendar()).isTrue();
            assertThat(resumo.isPodeFinalizar()).isFalse();
        }

        @Test
        @DisplayName("Falha com 'não encontrado' para agendamento inexistente")
        void falhaParaAgendamentoInexistente() {
            assertThatThrownBy(() -> service.buscar(999L)).isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("Confirma um agendamento")
        void confirma() {
            agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.AGENDADO);

            AgendamentoResumoDTO resumo = service.confirmar(100L);

            assertThat(resumo.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("Cancela com motivo")
        void cancelaComMotivo() {
            agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.AGENDADO);

            AgendamentoResumoDTO resumo = service.cancelar(100L, "Cliente desistiu");

            assertThat(resumo.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
            assertThat(resumo.getMotivoCancelamento()).isEqualTo("Cliente desistiu");
        }

        @Test
        @DisplayName("Não cancela dentro da janela de antecedência")
        void naoCancelaDentroDaJanela() {
            agendamentoExistente(SEGUNDA.atTime(11, 0), StatusAgendamento.AGENDADO);

            assertThatThrownBy(() -> service.cancelar(100L, "Imprevisto"))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("antecedência mínima");
        }

        @Test
        @DisplayName("Registrar falta soma uma falta ao cliente")
        void registrarFaltaSomaFaltaAoCliente() {
            agendamentoExistente(AGORA.minusHours(2), StatusAgendamento.CONFIRMADO);

            AgendamentoResumoDTO resumo = service.registrarFalta(100L);

            assertThat(resumo.getStatus()).isEqualTo(StatusAgendamento.FALTOU);
            assertThat(cliente.getFaltas()).isEqualTo(1);
        }

        @Test
        @DisplayName("Conclui um atendimento que já começou")
        void concluiAtendimentoQueJaComecou() {
            agendamentoExistente(AGORA.minusHours(2), StatusAgendamento.CONFIRMADO);

            AgendamentoResumoDTO resumo = service.concluir(100L);

            assertThat(resumo.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
        }
    }

    // ==================================================================
    // Reagendamento
    // ==================================================================

    @Nested
    @DisplayName("Reagendar")
    class Reagendar {

        @Test
        @DisplayName("Reagenda para um horário livre, conta o reagendamento e volta para Agendado")
        void reagendaParaHorarioLivre() {
            agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.CONFIRMADO);

            AgendamentoResumoDTO resumo = service.reagendar(100L, TERCA.atTime(16, 0));

            assertThat(resumo.getInicio()).isEqualTo(TERCA.atTime(16, 0));
            assertThat(resumo.getFim()).isEqualTo(TERCA.atTime(17, 0));
            assertThat(resumo.getVezesReagendado()).isEqualTo(1);
            assertThat(resumo.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        }

        @Test
        @DisplayName("Rejeita novo horário ocupado")
        void rejeitaHorarioOcupado() {
            agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.AGENDADO);
            when(agendamentoRepository.buscarConflitosDoProfissional(eq(2L), any(), any(), any(), eq(100L)))
                    .thenReturn(List.of(new Agendamento()));

            assertThatThrownBy(() -> service.reagendar(100L, TERCA.atTime(16, 0)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("já está ocupado");
        }

        @Test
        @DisplayName("Rejeita novo horário fora do expediente")
        void rejeitaHorarioForaDoExpediente() {
            agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.AGENDADO);

            assertThatThrownBy(() -> service.reagendar(100L, QUARTA.atTime(14, 0)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não atende nesse dia e horário");
        }

        @Test
        @DisplayName("Rejeita quando o limite de reagendamentos já foi atingido")
        void rejeitaQuandoAtingiuOLimite() {
            Agendamento existente = agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.AGENDADO);
            existente.setVezesReagendado(2);

            assertThatThrownBy(() -> service.reagendar(100L, TERCA.atTime(16, 0)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("limite");
        }

        @Test
        @DisplayName("Ao reagendar, o próprio horário atual não conta como ocupado nos horários livres")
        void horariosLivresIgnoramOProprioAgendamento() {
            agendamentoExistente(TERCA.atTime(14, 0), StatusAgendamento.AGENDADO);
            when(horarioTrabalhoRepository.findByProfissionalIdAndDiaSemanaOrderByHoraInicio(2L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(new HorarioTrabalho(profissional, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0))));

            // Sem ignorar: o repositório devolveria o próprio agendamento como conflito
            Agendamento proprio = new Agendamento();
            proprio.setInicio(TERCA.atTime(14, 0));
            proprio.setFim(TERCA.atTime(15, 0));
            when(agendamentoRepository.buscarConflitosDoProfissional(eq(2L), any(), any(), any(), eq(0L)))
                    .thenReturn(List.of(proprio));

            assertThat(service.horariosDisponiveis(2L, 10L, TERCA)).isEmpty();
            assertThat(service.horariosDisponiveis(2L, 10L, TERCA, 100L)).hasSize(1);
        }
    }
}
