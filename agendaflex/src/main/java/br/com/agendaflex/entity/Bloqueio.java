package br.com.agendaflex.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Período em que o profissional não atende (férias, consulta médica, reunião...). */
@Entity
@Table(name = "bloqueios")
@Getter
@Setter
@NoArgsConstructor
public class Bloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(nullable = false, length = 200)
    private String motivo;

    public Bloqueio(Profissional profissional, LocalDateTime inicio, LocalDateTime fim, String motivo) {
        this.profissional = profissional;
        this.inicio = inicio;
        this.fim = fim;
        this.motivo = motivo;
    }

    public boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }
}
