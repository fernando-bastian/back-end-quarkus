package br.com.fernandobastian.financeiro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario extends Entidade {
    @Column(nullable = false, length = 100)
    public String nome;

    @Column(nullable = false, unique = true, length = 254)
    public String email;

    @Column(name = "senha_hash", nullable = false)
    public String senhaHash;
}
