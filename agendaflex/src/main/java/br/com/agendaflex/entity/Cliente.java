package br.com.agendaflex.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false)
    private int faltas = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    public Cliente(String nome, String email, String telefone) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public void registrarFalta() {
        this.faltas++;
    }

    public void zerarFaltas() {
        this.faltas = 0;
    }

    /** Cliente com faltas demais fica impedido de fazer novos agendamentos. */
    public boolean estaBloqueado(int limiteFaltas) {
        return faltas >= limiteFaltas;
    }
}
