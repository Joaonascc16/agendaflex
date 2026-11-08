package br.com.agendaflex.service;

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
import br.com.agendaflex.util.TempoUtil;
import br.com.agendaflex.util.TextoUtil;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coração do sistema: concentra as regras de negócio do agendamento.
 *
 * Regras aplicadas ao criar um agendamento:
 *  1. Cliente ativo e sem excesso de faltas.
 *  2. Serviço e profissional ativos, e o profissional realiza o serviço.
 *  3. Horário no futuro, respeitando antecedência mínima e máxima.
 *  4. Horário alinhado ao passo da agenda (ex.: de 30 em 30 minutos).
 *  5. Atendimento dentro de uma janela de trabalho do profissional, no mesmo dia.
 *  6. Sem bloqueio (férias, folga...) e sem conflito com outro agendamento do profissional.
 *  7. Sem conflito com outro agendamento do próprio cliente.
 *  8. Limite de agendamentos ativos por cliente e de agendamentos por dia.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgendamentoService {

    private static final Long NENHUM = 0L;

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final RegrasAgendamentoProperties regras;
    private final Clock clock;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<AgendamentoResumoDTO> listar(StatusAgendamento status, Long profissionalId) {
        List<Agendamento> agendamentos;
        if (status != null && profissionalId != null) {
            agendamentos = agendamentoRepository.findByStatusAndProfissionalIdOrderByInicioDesc(status, profissionalId);
        } else if (status != null) {
            agendamentos = agendamentoRepository.findByStatusOrderByInicioDesc(status);
        } else if (profissionalId != null) {
            agendamentos = agendamentoRepository.findByProfissionalIdOrderByInicioDesc(profissionalId);
        } else {
            agendamentos = agendamentoRepository.findAllByOrderByInicioDesc();
        }
        return paraResumos(agendamentos);
    }

    public List<AgendamentoResumoDTO> listarPorCliente(Long clienteId) {
        return paraResumos(agendamentoRepository.findByClienteIdOrderByInicioDesc(clienteId));
    }

    public AgendamentoResumoDTO buscar(Long id) {
        return paraResumo(buscarEntidade(id), agora());
    }

    /** Horários livres do profissional para o serviço em uma data. */
    public List<SlotDTO> horariosDisponiveis(Long profissionalId, Long servicoId, LocalDate data) {
        return horariosDisponiveis(profissionalId, servicoId, data, NENHUM);
    }

    /**
     * Mesmo cálculo, ignorando um agendamento existente (usado no reagendamento,
     * para que o próprio horário atual não conte como ocupado).
     */
    public List<SlotDTO> horariosDisponiveis(Long profissionalId, Long servicoId, LocalDate data, Long ignorarAgendamentoId) {
        Profissional profissional = buscarProfissional(profissionalId);
        Servico servico = buscarServico(servicoId);

        if (!profissional.isAtivo() || !servico.isAtivo() || !profissional.atendeServico(servico)) {
            return List.of();
        }

        List<HorarioTrabalho> janelas = horarioTrabalhoRepository
                .findByProfissionalIdAndDiaSemanaOrderByHoraInicio(profissionalId, data.getDayOfWeek());
        if (janelas.isEmpty()) {
            return List.of();
        }

        LocalDateTime agora = agora();
        LocalDateTime primeiroPermitido = agora.plusMinutes(regras.getAntecedenciaMinimaMinutos());
        LocalDateTime ultimoPermitido = agora.plusDays(regras.getAntecedenciaMaximaDias());
        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.plusDays(1).atStartOfDay();

        List<Agendamento> ocupados = agendamentoRepository.buscarConflitosDoProfissional(
                profissionalId, StatusAgendamento.ATIVOS, inicioDoDia, fimDoDia, ignorarAgendamentoId);
        List<Bloqueio> bloqueios = bloqueioRepository.buscarSobrepostos(profissionalId, inicioDoDia, fimDoDia);

        int passo = regras.getPassoSlotMinutos();
        int duracao = servico.getDuracaoMinutos();
        List<SlotDTO> slots = new ArrayList<>();

        for (HorarioTrabalho janela : janelas) {
            int inicioJanela = janela.getHoraInicio().toSecondOfDay() / 60;
            int fimJanela = janela.getHoraFim().toSecondOfDay() / 60;
            // primeiro minuto da janela que cai na grade de N em N minutos
            int primeiro = ((inicioJanela + passo - 1) / passo) * passo;

            for (int minuto = primeiro; minuto + duracao <= fimJanela; minuto += passo) {
                LocalDateTime inicio = data.atTime(minuto / 60, minuto % 60);
                LocalDateTime fim = inicio.plusMinutes(duracao);

                if (inicio.isBefore(primeiroPermitido) || inicio.isAfter(ultimoPermitido)) {
                    continue;
                }
                boolean ocupado = ocupados.stream().anyMatch(a -> a.sobrepoe(inicio, fim));
                boolean bloqueado = bloqueios.stream().anyMatch(b -> b.sobrepoe(inicio, fim));
                if (!ocupado && !bloqueado) {
                    slots.add(new SlotDTO(inicio, fim));
                }
            }
        }
        return slots;
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    @Transactional
    public AgendamentoResumoDTO criar(AgendamentoDTO dto) {
        Cliente cliente = buscarCliente(dto.getClienteId());
        Profissional profissional = buscarProfissional(dto.getProfissionalId());
        Servico servico = buscarServico(dto.getServicoId());

        validarCadastros(cliente, profissional, servico);

        LocalDateTime inicio = dto.getInicio().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime fim = servico.calcularFim(inicio);

        validarDisponibilidade(cliente, profissional, inicio, fim, NENHUM);
        validarLimitesDoCliente(cliente, inicio);

        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setPrecoCobrado(servico.getPreco());
        agendamento.setObservacoes(TextoUtil.limparOuNulo(dto.getObservacoes()));

        Agendamento salvo = agendamentoRepository.save(agendamento);
        return paraResumo(salvo, agora());
    }

    @Transactional
    public AgendamentoResumoDTO confirmar(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        agendamento.confirmar();
        return paraResumo(agendamento, agora());
    }

    @Transactional
    public AgendamentoResumoDTO cancelar(Long id, String motivo) {
        Agendamento agendamento = buscarEntidade(id);
        agendamento.cancelar(motivo, agora(), regras.getAntecedenciaMinimaCancelamentoMinutos());
        return paraResumo(agendamento, agora());
    }

    @Transactional
    public AgendamentoResumoDTO concluir(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        agendamento.concluir(agora());
        return paraResumo(agendamento, agora());
    }

    /** Registra que o cliente não compareceu e soma uma falta ao cadastro dele. */
    @Transactional
    public AgendamentoResumoDTO registrarFalta(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        agendamento.registrarFalta(agora());
        agendamento.getCliente().registrarFalta();
        return paraResumo(agendamento, agora());
    }

    @Transactional
    public AgendamentoResumoDTO reagendar(Long id, LocalDateTime novoInicio) {
        Agendamento agendamento = buscarEntidade(id);
        LocalDateTime agora = agora();

        agendamento.validarPodeReagendar(agora,
                regras.getAntecedenciaMinimaCancelamentoMinutos(), regras.getMaximoReagendamentos());

        LocalDateTime inicio = novoInicio.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime fim = agendamento.getServico().calcularFim(inicio);

        validarDisponibilidade(agendamento.getCliente(), agendamento.getProfissional(), inicio, fim, agendamento.getId());

        agendamento.reagendar(inicio, fim, agora,
                regras.getAntecedenciaMinimaCancelamentoMinutos(), regras.getMaximoReagendamentos());
        return paraResumo(agendamento, agora);
    }

    // ------------------------------------------------------------------
    // Regras de validação
    // ------------------------------------------------------------------

    private void validarCadastros(Cliente cliente, Profissional profissional, Servico servico) {
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("Este cliente está inativo e não pode receber novos agendamentos.");
        }
        if (cliente.estaBloqueado(regras.getLimiteFaltasParaBloqueio())) {
            throw new RegraNegocioException("Este cliente atingiu o limite de " + regras.getLimiteFaltasParaBloqueio()
                    + " faltas e está bloqueado para novos agendamentos. "
                    + "Zere as faltas na ficha do cliente para liberar.");
        }
        if (!servico.isAtivo()) {
            throw new RegraNegocioException("O serviço \"" + servico.getNome() + "\" está inativo.");
        }
        if (!profissional.isAtivo()) {
            throw new RegraNegocioException(profissional.getNome() + " está inativo(a) e não recebe agendamentos.");
        }
        if (!profissional.atendeServico(servico)) {
            throw new RegraNegocioException(profissional.getNome() + " não realiza o serviço \"" + servico.getNome() + "\".");
        }
    }

    /** Regras que dependem de data, horário e agenda. Vale para criar e para reagendar. */
    private void validarDisponibilidade(Cliente cliente, Profissional profissional,
                                        LocalDateTime inicio, LocalDateTime fim, Long ignorarId) {
        LocalDateTime agora = agora();

        if (!inicio.isAfter(agora)) {
            throw new RegraNegocioException("Não é possível agendar em um horário que já passou.");
        }
        if (inicio.isBefore(agora.plusMinutes(regras.getAntecedenciaMinimaMinutos()))) {
            throw new RegraNegocioException("Os agendamentos exigem antecedência mínima de "
                    + TempoUtil.descreverMinutos(regras.getAntecedenciaMinimaMinutos()) + ".");
        }
        if (inicio.isAfter(agora.plusDays(regras.getAntecedenciaMaximaDias()))) {
            throw new RegraNegocioException("Só é possível agendar com até "
                    + regras.getAntecedenciaMaximaDias() + " dias de antecedência.");
        }
        if (!inicio.toLocalDate().equals(fim.toLocalDate())) {
            throw new RegraNegocioException("O atendimento precisa terminar no mesmo dia em que começa.");
        }

        int minutoDoDia = inicio.getHour() * 60 + inicio.getMinute();
        if (minutoDoDia % regras.getPassoSlotMinutos() != 0) {
            throw new RegraNegocioException("Escolha um horário de "
                    + regras.getPassoSlotMinutos() + " em " + regras.getPassoSlotMinutos() + " minutos.");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFim = fim.toLocalTime();
        boolean dentroDoExpediente = horarioTrabalhoRepository
                .findByProfissionalIdAndDiaSemanaOrderByHoraInicio(profissional.getId(), inicio.getDayOfWeek())
                .stream()
                .anyMatch(janela -> janela.cobre(horaInicio, horaFim));
        if (!dentroDoExpediente) {
            throw new RegraNegocioException(profissional.getNome() + " não atende nesse dia e horário.");
        }

        List<Bloqueio> bloqueios = bloqueioRepository.buscarSobrepostos(profissional.getId(), inicio, fim);
        if (!bloqueios.isEmpty()) {
            throw new RegraNegocioException(profissional.getNome()
                    + " está indisponível nesse período (" + bloqueios.get(0).getMotivo() + ").");
        }

        if (!agendamentoRepository.buscarConflitosDoProfissional(
                profissional.getId(), StatusAgendamento.ATIVOS, inicio, fim, ignorarId).isEmpty()) {
            throw new RegraNegocioException("Esse horário já está ocupado na agenda de " + profissional.getNome() + ".");
        }

        if (!agendamentoRepository.buscarConflitosDoCliente(
                cliente.getId(), StatusAgendamento.ATIVOS, inicio, fim, ignorarId).isEmpty()) {
            throw new RegraNegocioException("O cliente já tem outro agendamento que coincide com esse horário.");
        }
    }

    private void validarLimitesDoCliente(Cliente cliente, LocalDateTime inicio) {
        long ativosFuturos = agendamentoRepository.countByClienteIdAndStatusInAndInicioAfter(
                cliente.getId(), StatusAgendamento.ATIVOS, agora());
        if (ativosFuturos >= regras.getMaximoAgendamentosAtivosPorCliente()) {
            throw new RegraNegocioException("O cliente já tem " + ativosFuturos
                    + " agendamentos ativos, que é o limite permitido.");
        }

        LocalDate dia = inicio.toLocalDate();
        long noDia = agendamentoRepository.contarDoClienteNoPeriodo(
                cliente.getId(), StatusAgendamento.ATIVOS, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
        if (noDia >= regras.getMaximoAgendamentosPorClientePorDia()) {
            throw new RegraNegocioException("O cliente já tem " + noDia
                    + " agendamento(s) nesse dia, que é o limite diário.");
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private Agendamento buscarEntidade(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    private Profissional buscarProfissional(Long id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", id));
    }

    private Servico buscarServico(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }

    private List<AgendamentoResumoDTO> paraResumos(List<Agendamento> agendamentos) {
        LocalDateTime agora = agora();
        return agendamentos.stream().map(a -> paraResumo(a, agora)).toList();
    }

    /** Converte para DTO já calculando quais ações o usuário pode executar agora. */
    public AgendamentoResumoDTO paraResumo(Agendamento a, LocalDateTime agora) {
        AgendamentoResumoDTO dto = new AgendamentoResumoDTO();
        dto.setId(a.getId());
        dto.setClienteId(a.getCliente().getId());
        dto.setClienteNome(a.getCliente().getNome());
        dto.setProfissionalId(a.getProfissional().getId());
        dto.setProfissionalNome(a.getProfissional().getNome());
        dto.setServicoId(a.getServico().getId());
        dto.setServicoNome(a.getServico().getNome());
        dto.setInicio(a.getInicio());
        dto.setFim(a.getFim());
        dto.setStatus(a.getStatus());
        dto.setPreco(a.getPrecoCobrado());
        dto.setObservacoes(a.getObservacoes());
        dto.setMotivoCancelamento(a.getMotivoCancelamento());
        dto.setVezesReagendado(a.getVezesReagendado());

        int antecedencia = regras.getAntecedenciaMinimaCancelamentoMinutos();
        dto.setPodeConfirmar(a.getStatus() == StatusAgendamento.AGENDADO);
        dto.setPodeCancelar(a.podeCancelar(agora, antecedencia));
        dto.setPodeReagendar(a.podeReagendar(agora, antecedencia, regras.getMaximoReagendamentos()));
        dto.setPodeFinalizar(a.isAtivo() && a.iniciou(agora));
        return dto;
    }
}
