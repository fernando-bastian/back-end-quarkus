package br.com.fernandobastian.financeiro.service;

import br.com.fernandobastian.financeiro.api.ApiException;
import br.com.fernandobastian.financeiro.auth.UsuarioAtual;
import br.com.fernandobastian.financeiro.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import static br.com.fernandobastian.financeiro.api.FinanceiroDtos.*;

@ApplicationScoped
@Transactional
public class FinanceiroService {
    @Inject UsuarioAtual atual;
    @Inject EntityManager entityManager;

    public CadastroResposta criarConta(NomeEntrada request) {
        var conta = new Conta();
        conta.usuario = atual.obter();
        conta.nome = request.nome().strip();
        conta.persist();
        return new CadastroResposta(conta.id, conta.nome);
    }

    public List<CadastroResposta> contas() {
        return Conta.<Conta>list("usuario.id = ?1 order by id", atual.obter().id).stream()
                .map(c -> new CadastroResposta(c.id, c.nome)).toList();
    }

    public CadastroResposta detalharConta(long id) {
        Conta conta = conta(id, atual.obter().id);
        return new CadastroResposta(conta.id, conta.nome);
    }

    public CadastroResposta atualizarConta(long id, NomeEntrada request) {
        Conta conta = conta(id, atual.obter().id);
        conta.nome = request.nome().strip();
        return new CadastroResposta(conta.id, conta.nome);
    }

    public void excluirConta(long id) {
        Conta conta = conta(id, atual.obter().id);
        // o bloqueio impede um lançamento concorrente entre a consulta e a exclusão
        entityManager.createNativeQuery("SELECT id FROM contas WHERE id = :id FOR UPDATE")
                .setParameter("id", id).getResultList();
        if (Transacao.count("conta.id", id) > 0) {
            throw new ApiException(409, "Não é possível excluir uma conta com lançamentos.");
        }
        conta.delete();
    }

    public CadastroResposta criarCategoria(NomeEntrada request) {
        var categoria = new Categoria();
        categoria.usuario = atual.obter();
        categoria.nome = request.nome().strip();
        categoria.persist();
        return new CadastroResposta(categoria.id, categoria.nome);
    }

    public List<CadastroResposta> categorias() {
        return Categoria.<Categoria>list("usuario.id = ?1 order by id", atual.obter().id).stream()
                .map(c -> new CadastroResposta(c.id, c.nome)).toList();
    }

    public TransacaoResposta lancar(TransacaoEntrada request) {
        Usuario usuario = atual.obter();
        var transacao = novoLancamento(usuario, conta(request.contaId(), usuario.id),
                TipoLancamento.valueOf(request.tipo().name()), request.valor(), request.data(), request.descricao(), null);
        transacao.categoria = categoria(request.categoriaId(), usuario.id);
        transacao.persist();
        return TransacaoResposta.de(transacao);
    }

    public List<TransacaoResposta> transacoes(Long contaId, Long categoriaId) {
        Usuario usuario = atual.obter();
        var params = new HashMap<String, Object>();
        params.put("usuario", usuario.id);
        String query = "usuario.id = :usuario";
        if (contaId != null) {
            conta(contaId, usuario.id);
            params.put("conta", contaId);
            query += " and conta.id = :conta";
        }
        if (categoriaId != null) {
            categoria(categoriaId, usuario.id);
            params.put("categoria", categoriaId);
            query += " and categoria.id = :categoria";
        }
        return Transacao.<Transacao>list(query + " order by data desc, id desc", params)
                .stream().map(TransacaoResposta::de).toList();
    }

    public TransferenciaResposta transferir(TransferenciaEntrada request) {
        Usuario usuario = atual.obter();
        if (request.contaOrigemId().equals(request.contaDestinoId())) {
            throw new ApiException(400, "As contas de origem e destino devem ser diferentes.");
        }
        Conta origem = conta(request.contaOrigemId(), usuario.id);
        Conta destino = conta(request.contaDestinoId(), usuario.id);
        UUID id = UUID.randomUUID();
        novoLancamento(usuario, origem, TipoLancamento.TRANSFERENCIA_SAIDA,
                request.valor(), request.data(), request.descricao(), id).persist();
        novoLancamento(usuario, destino, TipoLancamento.TRANSFERENCIA_ENTRADA,
                request.valor(), request.data(), request.descricao(), id).persist();
        return new TransferenciaResposta(id, origem.id, destino.id,
                request.valor().setScale(2), request.data(), request.descricao());
    }

    public SaldosResposta saldos() {
        Long usuarioId = atual.obter().id;
        // calcula os saldos na mesma leitura, sem pegar uma transferência pela metade
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT c.id, c.nome,
                       COALESCE(SUM(CASE WHEN t.tipo IN ('RECEITA', 'TRANSFERENCIA_ENTRADA')
                                         THEN t.valor ELSE -t.valor END), 0)
                  FROM contas c
                  LEFT JOIN transacoes t ON t.conta_id = c.id AND t.usuario_id = c.usuario_id
                 WHERE c.usuario_id = :usuario
                 GROUP BY c.id, c.nome
                 ORDER BY c.id
                """).setParameter("usuario", usuarioId).getResultList();
        var contas = new ArrayList<SaldoConta>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Object[] row : rows) {
            BigDecimal saldo = ((BigDecimal) row[2]).setScale(2);
            contas.add(new SaldoConta(((Number) row[0]).longValue(), (String) row[1], saldo));
            total = total.add(saldo);
        }
        return new SaldosResposta(contas, total);
    }

    private Conta conta(Long id, Long usuarioId) {
        Conta conta = Conta.find("id = ?1 and usuario.id = ?2", id, usuarioId).firstResult();
        if (conta == null) throw new ApiException(404, "Conta não encontrada.");
        return conta;
    }

    private Categoria categoria(Long id, Long usuarioId) {
        Categoria categoria = Categoria.find("id = ?1 and usuario.id = ?2", id, usuarioId).firstResult();
        if (categoria == null) throw new ApiException(404, "Categoria não encontrada.");
        return categoria;
    }

    private Transacao novoLancamento(Usuario usuario, Conta conta, TipoLancamento tipo,
                                     BigDecimal valor, LocalDate data, String descricao, UUID transferenciaId) {
        var transacao = new Transacao();
        transacao.usuario = usuario;
        transacao.conta = conta;
        transacao.tipo = tipo;
        transacao.valor = valor.setScale(2);
        transacao.data = data;
        transacao.descricao = descricao == null ? null : descricao.strip();
        transacao.transferenciaId = transferenciaId;
        return transacao;
    }
}
