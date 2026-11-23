package br.com.fernandobastian.financeiro;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(TestEnvironment.class)
@TestProfile(TestDatabaseProfile.class)
class FinanceiroApiTest extends ApiSupport {
    @Test
    void forneceCsrfAntesDoLogin() {
        given().when().get("/api/auth/csrf").then()
                .statusCode(200).body("token", not(emptyOrNullString()))
                .cookie("XSRF-TOKEN", not(emptyOrNullString()));
    }

    @Test
    void cadastraConsultaContasECategoriasComIsolamento() {
        Client ana = login("ana@exemplo.com");
        long contaId = conta(ana, "  Carteira  ");
        long categoriaId = categoria(ana, "  Alimentação  ");
        ana.request().get("/api/contas").then().statusCode(200)
                .body("size()", is(1)).body("[0].id", is((int) contaId)).body("[0].nome", is("Carteira"));
        ana.request().get("/api/categorias").then().statusCode(200)
                .body("size()", is(1)).body("[0].id", is((int) categoriaId)).body("[0].nome", is("Alimentação"));
        Client bruno = login("bruno@exemplo.com");
        bruno.request().get("/api/contas").then().statusCode(200).body("size()", is(0));
        bruno.request().get("/api/categorias").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void validaNomesEJson() {
        Client ana = login("ana@exemplo.com");
        for (String path : new String[]{"contas", "categorias"}) {
            for (String json : new String[]{"{}", "null", "{\"nome\":\"   \"}",
                    "{\"nome\":\"" + "x".repeat(101) + "\"}", "{\"nome\":\"Teste\",\"usuarioId\":2}", "{"}) {
                ana.request().body(json).post("/api/" + path).then().statusCode(400)
                        .body("mensagem", not(emptyOrNullString()));
            }
        }
    }

    @Test
    void lancaReceitasDespesasECalculaSaldosExatos() {
        Client ana = login("ana@exemplo.com");
        long carteira = conta(ana, "Carteira");
        long banco = conta(ana, "Banco");
        long categoria = categoria(ana, "Geral");
        lancar(ana, carteira, categoria, "RECEITA", "100.10").then().statusCode(201);
        lancar(ana, carteira, categoria, "DESPESA", "30.05").then().statusCode(201);
        lancar(ana, banco, categoria, "RECEITA", "0.20").then().statusCode(201);
        conta(ana, "Vazia");
        var saldos = ana.request().get("/api/saldos").then().statusCode(200).extract().jsonPath()
                .using(new io.restassured.path.json.config.JsonPathConfig(io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        assertEquals(new BigDecimal("70.25"), saldos.getObject("saldoGeral", BigDecimal.class));
        assertEquals(new BigDecimal("70.05"), saldos.getObject("contas[0].saldo", BigDecimal.class));
        assertEquals(new BigDecimal("0.20"), saldos.getObject("contas[1].saldo", BigDecimal.class));
        assertEquals(new BigDecimal("0.00"), saldos.getObject("contas[2].saldo", BigDecimal.class));
        ana.request().get("/api/transacoes").then().statusCode(200).body("size()", is(3));
        Client bruno = login("bruno@exemplo.com");
        bruno.request().get("/api/saldos").then().statusCode(200)
                .body("saldoGeral", is(0.0f)).body("contas.size()", is(0));
        bruno.request().get("/api/transacoes").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void filtraPorContaCategoriaEAmbas() {
        Client ana = login("ana@exemplo.com");
        long c1 = conta(ana, "Conta 1"), c2 = conta(ana, "Conta 2");
        long k1 = categoria(ana, "Categoria 1"), k2 = categoria(ana, "Categoria 2");
        lancar(ana, c1, k1, "RECEITA", "10.00").then().statusCode(201);
        lancar(ana, c1, k2, "DESPESA", "2.00").then().statusCode(201);
        lancar(ana, c2, k1, "RECEITA", "20.00").then().statusCode(201);
        ana.request().queryParam("contaId", c1).get("/api/transacoes").then().statusCode(200).body("size()", is(2));
        ana.request().queryParam("categoriaId", k1).get("/api/transacoes").then().statusCode(200).body("size()", is(2));
        ana.request().queryParam("contaId", c1).queryParam("categoriaId", k1).get("/api/transacoes")
                .then().statusCode(200).body("size()", is(1)).body("[0].tipo", is("RECEITA"));
        ana.request().queryParam("contaId", "abc").get("/api/transacoes").then().statusCode(400);
        ana.request().queryParam("contaId", 0).get("/api/transacoes").then().statusCode(400);
    }

    @Test
    void rejeitaReferenciasAlheiasOuInexistentes() {
        Client ana = login("ana@exemplo.com"), bruno = login("bruno@exemplo.com");
        long contaAna = conta(ana, "Ana"), contaBruno = conta(bruno, "Bruno");
        long categoriaAna = categoria(ana, "Ana"), categoriaBruno = categoria(bruno, "Bruno");
        lancar(bruno, contaAna, categoriaBruno, "RECEITA", "10.00").then().statusCode(404);
        lancar(bruno, contaBruno, categoriaAna, "DESPESA", "10.00").then().statusCode(404);
        lancar(bruno, 99999, categoriaBruno, "RECEITA", "10.00").then().statusCode(404);
        bruno.request().queryParam("contaId", contaAna).get("/api/transacoes").then().statusCode(404);
        bruno.request().queryParam("categoriaId", categoriaAna).get("/api/transacoes").then().statusCode(404);
        bruno.request().get("/api/transacoes").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void validaValoresDatasTipoECamposObrigatorios() {
        Client ana = login("ana@exemplo.com");
        long conta = conta(ana, "Carteira"), categoria = categoria(ana, "Geral");
        for (String valor : new String[]{"0", "-0.01", "1.001", "100000000000000000.00"}) {
            lancar(ana, conta, categoria, "RECEITA", valor).then().statusCode(400)
                    .body("mensagem", not(emptyOrNullString()));
        }
        lancar(ana, conta, categoria, "TRANSFERENCIA_ENTRADA", "10.00").then().statusCode(400);
        for (String json : new String[]{"{}", "null", "{\"tipo\":\"INVALIDO\"}",
                "{\"contaId\":1,\"categoriaId\":1,\"tipo\":\"RECEITA\",\"valor\":1,\"data\":\"2026-02-30\"}"}) {
            ana.request().body(json).post("/api/transacoes").then().statusCode(400);
        }
        ana.request().get("/api/transacoes").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void rejeitaIdsFracionariosETipoNumericoSemConverterSilenciosamente() {
        Client ana = login("ana@exemplo.com");
        long conta = conta(ana, "Carteira"), categoria = categoria(ana, "Geral");
        String base = "{\"contaId\":%s,\"categoriaId\":%s,\"tipo\":%s,\"valor\":1.00,\"data\":\"2026-11-16\"}";
        for (String json : new String[]{
                base.formatted(conta + ".9", categoria, "\"RECEITA\""),
                base.formatted(conta, categoria + ".9", "\"DESPESA\""),
                base.formatted(conta, categoria, "0")}) {
            ana.request().body(json).post("/api/transacoes").then().statusCode(400)
                    .body("mensagem", not(emptyOrNullString()));
        }
        ana.request().get("/api/transacoes").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void rejeitaIdsFracionariosNaTransferencia() {
        Client ana = login("ana@exemplo.com");
        long origem = conta(ana, "Origem"), destino = conta(ana, "Destino");
        String base = "{\"contaOrigemId\":%s,\"contaDestinoId\":%s,\"valor\":1.00,\"data\":\"2026-11-16\"}";
        for (String json : new String[]{base.formatted(origem + ".9", destino), base.formatted(origem, destino + ".9")}) {
            ana.request().body(json).post("/api/transferencias").then().statusCode(400)
                    .body("mensagem", not(emptyOrNullString()));
        }
        ana.request().get("/api/transacoes").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void preservaPrecisaoAlemDoLimiteDeDouble() {
        Client ana = login("ana@exemplo.com");
        long conta = conta(ana, "Banco"), categoria = categoria(ana, "Geral");
        lancar(ana, conta, categoria, "RECEITA", new BigDecimal("9007199254740993.01")).then().statusCode(201);
        lancar(ana, conta, categoria, "DESPESA", new BigDecimal("0.02")).then().statusCode(201);
        var saldos = ana.request().get("/api/saldos").then().statusCode(200).extract().jsonPath()
                .using(new io.restassured.path.json.config.JsonPathConfig(io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        assertEquals(new BigDecimal("9007199254740992.99"), saldos.getObject("saldoGeral", BigDecimal.class));
    }

    @Test
    void transfereEntreContasSemAlterarSaldoGeral() {
        Client ana = login("ana@exemplo.com");
        long origem = conta(ana, "Origem"), destino = conta(ana, "Destino"), categoria = categoria(ana, "Geral");
        lancar(ana, origem, categoria, "RECEITA", "100.00").then().statusCode(201);
        ana.request().body(Map.of("contaOrigemId", origem, "contaDestinoId", destino,
                        "valor", "25.35", "data", "2026-11-16"))
                .post("/api/transferencias").then().statusCode(201)
                .body("id", not(emptyOrNullString())).body("contaOrigemId", is((int) origem));
        var saldos = ana.request().get("/api/saldos").then().statusCode(200).extract().jsonPath()
                .using(new io.restassured.path.json.config.JsonPathConfig(io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        assertEquals(new BigDecimal("100.00"), saldos.getObject("saldoGeral", BigDecimal.class));
        assertEquals(new BigDecimal("74.65"), saldos.getObject("contas[0].saldo", BigDecimal.class));
        assertEquals(new BigDecimal("25.35"), saldos.getObject("contas[1].saldo", BigDecimal.class));
        ana.request().queryParam("contaId", destino).get("/api/transacoes").then().statusCode(200)
                .body("size()", is(1)).body("[0].tipo", is("TRANSFERENCIA_ENTRADA"))
                .body("[0].categoriaId", nullValue()).body("[0].transferenciaId", notNullValue());
    }

    @Test
    void rejeitaTransferenciaParaMesmaContaAlheiaOuComValorInvalido() {
        Client ana = login("ana@exemplo.com"), bruno = login("bruno@exemplo.com");
        long origem = conta(ana, "Origem"), destino = conta(ana, "Destino"), alheia = conta(bruno, "Bruno");
        ana.request().body(Map.of("contaOrigemId", origem, "contaDestinoId", origem, "valor", "1.00", "data", "2026-11-16"))
                .post("/api/transferencias").then().statusCode(400);
        for (long contaAlheia : new long[]{alheia, 99999}) {
            ana.request().body(Map.of("contaOrigemId", origem, "contaDestinoId", contaAlheia, "valor", "1.00", "data", "2026-11-16"))
                    .post("/api/transferencias").then().statusCode(404);
            ana.request().body(Map.of("contaOrigemId", contaAlheia, "contaDestinoId", destino, "valor", "1.00", "data", "2026-11-16"))
                    .post("/api/transferencias").then().statusCode(404);
        }
        for (String valor : new String[]{"0", "-1", "1.001"}) {
            ana.request().body(Map.of("contaOrigemId", origem, "contaDestinoId", destino, "valor", valor, "data", "2026-11-16"))
                    .post("/api/transferencias").then().statusCode(400);
        }
        ana.request().get("/api/transacoes").then().statusCode(200).body("size()", is(0));
    }

    @Test
    void reverteDebitoSeCreditoFalharNoBanco() throws Exception {
        Client ana = login("ana@exemplo.com");
        long origem = conta(ana, "Origem"), destino = conta(ana, "Destino");
        sql("CREATE OR REPLACE FUNCTION falhar_credito() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN IF NEW.tipo = 'TRANSFERENCIA_ENTRADA' THEN RAISE EXCEPTION 'Falha de teste'; END IF; RETURN NEW; END $$");
        sql("CREATE TRIGGER falha_transferencia BEFORE INSERT ON transacoes FOR EACH ROW EXECUTE FUNCTION falhar_credito()");
        try {
            ana.request().body(Map.of("contaOrigemId", origem, "contaDestinoId", destino,
                            "valor", "10.00", "data", "2026-11-16"))
                    .post("/api/transferencias").then().statusCode(500).body("mensagem", not(emptyOrNullString()));
            ana.request().get("/api/transacoes").then().statusCode(200).body("size()", is(0));
            ana.request().get("/api/saldos").then().statusCode(200).body("saldoGeral", is(0.0f));
        } finally {
            sql("DROP TRIGGER falha_transferencia ON transacoes");
        }
    }
}
