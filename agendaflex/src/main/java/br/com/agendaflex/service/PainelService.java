package br.com.agendaflex.service;

import br.com.agendaflex.config.RegrasAgendamentoProperties;
import br.com.agendaflex.dtos.AgendaSemanaDTO;
import br.com.agendaflex.dtos.AgendamentoResumoDTO;
import br.com.agendaflex.dtos.BlocoAgendaDTO;
import br.com.agendaflex.dtos.DiaAgendaDTO;
import br.com.agendaflex.dtos.PainelDTO;
import br.com.agendaflex.entity.Agendamento;
import br.com.agendaflex.entity.Bloqueio;
import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.exception.RecursoNaoEncontradoException;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.BloqueioRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicadores do painel inicial e montagem da agenda semanal. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PainelService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoService agendamentoService;
    private final RegrasAgendamentoProperties regras;
    private final Clock clock;
    private final TenantContext tenantContext;

    public PainelDTO resumo() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDate hoje = agora.toLocalDate();
        LocalDate primeiroDoMes = hoje.withDayOfMonth(1);
        LocalDate primeiroDoProximoMes = primeiroDoMes.plusMonths(1);

        List<StatusAgendamento> contamHoje = List.of(
                StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.CONCLUIDO);

        Long estabelecimentoId = tenantContext.obterEstabelecimentoId();
        PainelDTO painel = new PainelDTO();
        painel.setHoje(hoje);
        painel.setAtendimentosHoje(agendamentoRepository.contarPorStatusNoPeriodoNoEstabelecimento(
                estabelecimentoId, contamHoje, hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay()));
        painel.setAguardandoConfirmacao(agendamentoRepository.contarPorStatusNoPeriodoNoEstabelecimento(
                estabelecimentoId, List.of(StatusAgendamento.AGENDADO), agora, agora.plusDays(regras.getAntecedenciaMaximaDias() + 1L)));
        painel.setProximosSeteDias(agendamentoRepository.contarPorStatusNoPeriodoNoEstabelecimento(
                estabelecimentoId, StatusAgendamento.ATIVOS, agora, agora.plusDays(7)));
        painel.setFaltasNoMes(agendamentoRepository.contarPorStatusNoPeriodoNoEstabelecimento(
                estabelecimentoId, List.of(StatusAgendamento.FALTOU), primeiroDoMes.atStartOfDay(), primeiroDoProximoMes.atStartOfDay()));

        BigDecimal receita = agendamentoRepository.somarReceitaNoEstabelecimento(
                estabelecimentoId, StatusAgendamento.CONCLUIDO, primeiroDoMes.atStartOfDay(), primeiroDoProximoMes.atStartOfDay());
        painel.setReceitaDoMes(receita == null ? BigDecimal.ZERO : receita);

        List<AgendamentoResumoDTO> proximos = agendamentoRepository
                .findTop5ByEstabelecimentoIdAndStatusInAndInicioAfterOrderByInicioAsc(
                        estabelecimentoId, StatusAgendamento.ATIVOS, agora)
                .stream()
                .map(a -> agendamentoService.paraResumo(a, agora))
                .toList();
        painel.setProximos(proximos);
        return painel;
    }

    /**
     * Agenda de uma semana (segunda a domingo) de um profissional.
     * Na grade, 1 minuto = 1 pixel, o que permite posicionar cada bloco só com topo e altura.
     */
    public AgendaSemanaDTO montarSemana(Long profissionalId, LocalDate referencia) {
        Long estabelecimentoId = tenantContext.obterEstabelecimentoId();
        if (profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Profissional", profissionalId);
        }

        LocalDate hoje = LocalDate.now(clock);
        LocalDate base = referencia != null ? referencia : hoje;
        LocalDate segunda = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate proximaSegunda = segunda.plusDays(7);

        List<Agendamento> agendamentos = agendamentoRepository
                .buscarNoPeriodoDoProfissionalNoEstabelecimento(
                        estabelecimentoId, profissionalId, segunda.atStartOfDay(), proximaSegunda.atStartOfDay())
                .stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .toList();
        List<Bloqueio> bloqueios = bloqueioRepository.buscarSobrepostosNoEstabelecimento(
                estabelecimentoId, profissionalId, segunda.atStartOfDay(), proximaSegunda.atStartOfDay());

        AgendaSemanaDTO semana = new AgendaSemanaDTO();
        semana.setProfissionalId(profissionalId);
        semana.setInicioSemana(segunda);
        semana.setFimSemana(segunda.plusDays(6));
        semana.setSemanaAnterior(segunda.minusDays(7));
        semana.setProximaSemana(proximaSegunda);

        int horaInicial = regras.getHoraInicialAgenda();
        int horaFinal = regras.getHoraFinalAgenda();
        List<String> horas = new ArrayList<>();
        for (int h = horaInicial; h < horaFinal; h++) {
            horas.add(String.format("%02d:00", h));
        }
        semana.setHoras(horas);
        semana.setAlturaPx((horaFinal - horaInicial) * 60);

        for (int i = 0; i < 7; i++) {
            LocalDate dia = segunda.plusDays(i);
            DiaAgendaDTO diaDto = new DiaAgendaDTO();
            diaDto.setData(dia);
            diaDto.setHoje(dia.equals(hoje));

            for (Agendamento a : agendamentos) {
                posicionar(dia, a.getInicio(), a.getFim()).ifPresent(pos -> {
                    BlocoAgendaDTO bloco = new BlocoAgendaDTO();
                    bloco.setId(a.getId());
                    bloco.setTitulo(a.getCliente().getNome());
                    bloco.setSubtitulo(a.getServico().getNome());
                    bloco.setHorario(a.getInicio().format(HORA) + " às " + a.getFim().format(HORA));
                    bloco.setStatus(a.getStatus());
                    bloco.setBloqueio(false);
                    bloco.setTopoPx(pos[0]);
                    bloco.setAlturaPx(pos[1]);
                    diaDto.getBlocos().add(bloco);
                });
            }

            for (Bloqueio b : bloqueios) {
                posicionar(dia, b.getInicio(), b.getFim()).ifPresent(pos -> {
                    BlocoAgendaDTO bloco = new BlocoAgendaDTO();
                    bloco.setTitulo("Indisponível");
                    bloco.setSubtitulo(b.getMotivo());
                    bloco.setHorario(b.getInicio().format(HORA) + " às " + b.getFim().format(HORA));
                    bloco.setBloqueio(true);
                    bloco.setTopoPx(pos[0]);
                    bloco.setAlturaPx(pos[1]);
                    diaDto.getBlocos().add(bloco);
                });
            }
            semana.getDias().add(diaDto);
        }
        return semana;
    }

    /**
     * Recorta o intervalo ao dia e à janela visível da grade.
     * Retorna {topo, altura} em pixels, ou vazio se o trecho não aparece nesse dia.
     */
    private Optional<int[]> posicionar(LocalDate dia, LocalDateTime inicio, LocalDateTime fim) {
        int gradeInicio = regras.getHoraInicialAgenda() * 60;
        int gradeFim = regras.getHoraFinalAgenda() * 60;

        LocalDateTime inicioDoDia = dia.atStartOfDay();
        LocalDateTime fimDoDia = dia.plusDays(1).atStartOfDay();
        LocalDateTime trechoInicio = inicio.isAfter(inicioDoDia) ? inicio : inicioDoDia;
        LocalDateTime trechoFim = fim.isBefore(fimDoDia) ? fim : fimDoDia;

        int minutoInicial = (int) Duration.between(inicioDoDia, trechoInicio).toMinutes();
        int minutoFinal = (int) Duration.between(inicioDoDia, trechoFim).toMinutes();

        int topo = Math.max(minutoInicial, gradeInicio);
        int base = Math.min(minutoFinal, gradeFim);
        if (base <= topo) {
            return Optional.empty();
        }
        return Optional.of(new int[] {topo - gradeInicio, base - topo});
    }
}
