package br.com.agendaflex.entity;

import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.util.TempoUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agendamentos", indexes = {
        @Index(name = "idx_agendamento_profissional_inicio", columnList = "profissional_id, inicio"),
        @Index(name = "idx_agendamento_cliente_inicio", columnList = "cliente_id, inicio")
})
@Getter
@Setter
@NoArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    /** Preço do serviço no momento do agendamento (reajustes futuros não alteram o histórico). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoCobrado;

    @Column(length = 500)
    private String observacoes;

    @Column(length = 200)
    private String motivoCancelamento;

    @Column(nullable = false)
    private int vezesReagendado = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // ------------------------------------------------------------------
    // Regras de domínio
    // ------------------------------------------------------------------

    public boolean isAtivo() {
        return status.isAtivo();
    }

    /** Dois atendimentos se sobrepõem quando um começa antes de o outro terminar. */
    public boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }

    public boolean iniciou(LocalDateTime agora) {
        return !agora.isBefore(inicio);
    }

    public boolean podeCancelar(LocalDateTime agora, int antecedenciaMinimaMinutos) {
        return isAtivo() && !agora.plusMinutes(antecedenciaMinimaMinutos).isAfter(inicio);
    }

    public boolean podeReagendar(LocalDateTime agora, int antecedenciaMinimaMinutos, int maximoReagendamentos) {
        return podeCancelar(agora, antecedenciaMinimaMinutos) && vezesReagendado < maximoReagendamentos;
    }

    public void confirmar() {
        mudarPara(StatusAgendamento.CONFIRMADO);
    }

    /** Só é possível concluir um atendimento depois que ele começou. */
    public void concluir(LocalDateTime agora) {
        exigirQueTenhaIniciado(agora, "concluir");
        mudarPara(StatusAgendamento.CONCLUIDO);
    }

    public void registrarFalta(LocalDateTime agora) {
        exigirQueTenhaIniciado(agora, "registrar falta em");
        mudarPara(StatusAgendamento.FALTOU);
    }

    public void cancelar(String motivo, LocalDateTime agora, int antecedenciaMinimaMinutos) {
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Informe o motivo do cancelamento.");
        }
        if (!status.podeTransitarPara(StatusAgendamento.CANCELADO)) {
            throw new RegraNegocioException("Um agendamento " + status.getDescricao().toLowerCase() + " não pode ser cancelado.");
        }
        if (agora.plusMinutes(antecedenciaMinimaMinutos).isAfter(inicio)) {
            throw new RegraNegocioException("Cancelamentos exigem antecedência mínima de "
                    + TempoUtil.descreverMinutos(antecedenciaMinimaMinutos) + ".");
        }
        this.status = StatusAgendamento.CANCELADO;
        this.motivoCancelamento = motivo.trim();
    }

    /** Verifica as regras de reagendamento que dependem só deste agendamento. */
    public void validarPodeReagendar(LocalDateTime agora, int antecedenciaMinimaMinutos, int maximoReagendamentos) {
        if (!isAtivo()) {
            throw new RegraNegocioException("Só é possível reagendar agendamentos ativos.");
        }
        if (vezesReagendado >= maximoReagendamentos) {
            throw new RegraNegocioException("Este agendamento já foi reagendado " + vezesReagendado
                    + " vez(es), que é o limite permitido.");
        }
        if (agora.plusMinutes(antecedenciaMinimaMinutos).isAfter(inicio)) {
            throw new RegraNegocioException("Reagendamentos exigem antecedência mínima de "
                    + TempoUtil.descreverMinutos(antecedenciaMinimaMinutos) + ".");
        }
    }

    /** Move o atendimento para outro horário. Depois de reagendado, precisa ser confirmado de novo. */
    public void reagendar(LocalDateTime novoInicio, LocalDateTime novoFim, LocalDateTime agora,
                          int antecedenciaMinimaMinutos, int maximoReagendamentos) {
        validarPodeReagendar(agora, antecedenciaMinimaMinutos, maximoReagendamentos);
        this.inicio = novoInicio;
        this.fim = novoFim;
        this.vezesReagendado++;
        this.status = StatusAgendamento.AGENDADO;
    }

    private void mudarPara(StatusAgendamento destino) {
        if (!status.podeTransitarPara(destino)) {
            throw new RegraNegocioException("Não é possível mudar o agendamento de "
                    + status.getDescricao().toLowerCase() + " para " + destino.getDescricao().toLowerCase() + ".");
        }
        this.status = destino;
    }

    private void exigirQueTenhaIniciado(LocalDateTime agora, String acao) {
        if (!iniciou(agora)) {
            throw new RegraNegocioException("Só é possível " + acao + " um atendimento depois do horário de início.");
        }
    }
}
