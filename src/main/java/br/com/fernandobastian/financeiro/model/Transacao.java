package br.com.fernandobastian.financeiro.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transacoes")
public class Transacao extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    public Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    public Conta conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    public Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public TipoLancamento tipo;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal valor;

    @Column(nullable = false)
    public LocalDate data;

    @Column(length = 255)
    public String descricao;

    @Column(name = "transferencia_id")
    public UUID transferenciaId;
}
