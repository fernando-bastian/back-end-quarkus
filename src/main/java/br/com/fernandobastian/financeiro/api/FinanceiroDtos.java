package br.com.fernandobastian.financeiro.api;

import br.com.fernandobastian.financeiro.model.Transacao;
import br.com.fernandobastian.financeiro.model.TipoLancamento;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FinanceiroDtos {
    private FinanceiroDtos() {}

    public enum Natureza { RECEITA, DESPESA }

    public record NomeEntrada(@NotBlank(message = "Informe o nome.")
                              @Size(max = 100, message = "O nome deve ter até 100 caracteres.") String nome) {}

    public record CadastroResposta(Long id, String nome) {}

    public record TransacaoEntrada(
            @NotNull @Positive Long contaId,
            @NotNull @Positive Long categoriaId,
            @NotNull Natureza tipo,
            @NotNull @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
            @Digits(integer = 17, fraction = 2, message = "Use até 17 dígitos inteiros e duas casas decimais.") BigDecimal valor,
            @NotNull LocalDate data,
            @Size(max = 255) String descricao) {}

    public record TransferenciaEntrada(
            @NotNull @Positive Long contaOrigemId,
            @NotNull @Positive Long contaDestinoId,
            @NotNull @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
            @Digits(integer = 17, fraction = 2, message = "Use até 17 dígitos inteiros e duas casas decimais.") BigDecimal valor,
            @NotNull LocalDate data,
            @Size(max = 255) String descricao) {}

    public record TransacaoResposta(Long id, Long contaId, Long categoriaId, TipoLancamento tipo,
                                   BigDecimal valor, LocalDate data, String descricao, UUID transferenciaId) {
        public static TransacaoResposta de(Transacao transacao) {
            return new TransacaoResposta(transacao.id, transacao.conta.id,
                    transacao.categoria == null ? null : transacao.categoria.id, transacao.tipo,
                    transacao.valor, transacao.data, transacao.descricao, transacao.transferenciaId);
        }
    }

    public record TransferenciaResposta(UUID id, Long contaOrigemId, Long contaDestinoId,
                                       BigDecimal valor, LocalDate data, String descricao) {}

    public record SaldoConta(Long contaId, String nome, BigDecimal saldo) {}

    public record SaldosResposta(List<SaldoConta> contas, BigDecimal saldoGeral) {}
}
