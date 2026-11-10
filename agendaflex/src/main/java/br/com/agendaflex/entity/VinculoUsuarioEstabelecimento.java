package br.com.agendaflex.entity;

import br.com.agendaflex.enums.PapelEstabelecimento;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vinculos_usuario_estabelecimento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_usuario_estabelecimento",
                columnNames = {"usuario_id", "estabelecimento_id"}),
        indexes = @Index(name = "idx_vinculo_estabelecimento_papel",
                columnList = "estabelecimento_id, papel"))
@Getter
@Setter
@NoArgsConstructor
public class VinculoUsuarioEstabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelEstabelecimento papel;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public VinculoUsuarioEstabelecimento(Usuario usuario, Estabelecimento estabelecimento,
                                         PapelEstabelecimento papel) {
        this.usuario = usuario;
        this.estabelecimento = estabelecimento;
        this.papel = papel;
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }
}
