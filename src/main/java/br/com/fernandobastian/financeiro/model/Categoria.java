package br.com.fernandobastian.financeiro.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias")
public class Categoria extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    public Usuario usuario;

    @Column(nullable = false, length = 100)
    public String nome;
}
