package br.com.fernandobastian.financeiro;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(TestEnvironment.class)
@TestProfile(TestDatabaseProfile.class)
class ContasApiTest extends ApiSupport {
    @Test
    void detalhaEditaEExcluiContaVazia() {
        Client ana = login("ana@exemplo.com");
        long id = conta(ana, "Carteira");
        ana.request().get("/api/contas/" + id).then().statusCode(200).body("nome", is("Carteira"));
        ana.request().body(Map.of("nome", "  Reserva  ")).put("/api/contas/" + id)
                .then().statusCode(200).body("id", is((int) id)).body("nome", is("Reserva"));
        ana.request().get("/api/contas").then().body("[0].nome", is("Reserva"));
        ana.request().delete("/api/contas/" + id).then().statusCode(204);
        ana.request().get("/api/contas").then().body("size()", is(0));
        ana.request().get("/api/saldos").then().body("contas.size()", is(0));
    }

    @Test
    void protegeContasDeOutroUsuarioEIdsInexistentes() {
        Client ana = login("ana@exemplo.com"), bruno = login("bruno@exemplo.com");
        long id = conta(ana, "Carteira");
        for (long alvo : new long[]{id, 99999}) {
            bruno.request().get("/api/contas/" + alvo).then().statusCode(404);
            bruno.request().body(Map.of("nome", "Alterada")).put("/api/contas/" + alvo).then().statusCode(404);
            bruno.request().delete("/api/contas/" + alvo).then().statusCode(404);
        }
        ana.request().get("/api/contas").then().body("[0].nome", is("Carteira"));
    }

    @Test
    void validaEdicaoEExigeCsrfEAutenticacao() {
        Client ana = login("ana@exemplo.com");
        long id = conta(ana, "Carteira");
        String path = "/api/contas/" + id;
        for (String json : new String[]{"{}", "null", "{\"nome\":\"  \"}",
                "{\"nome\":\"" + "x".repeat(101) + "\"}", "{\"nome\":\"Teste\",\"usuarioId\":2}"}) {
            ana.request().body(json).put(path).then().statusCode(400);
        }
        given().cookies(ana.cookies()).contentType("application/json").body(Map.of("nome", "Teste"))
                .put(path).then().statusCode(400);
        given().cookies(ana.cookies()).delete(path).then().statusCode(400);
        Client anonimo = csrf();
        anonimo.request().get(path).then().statusCode(401);
        anonimo.request().body(Map.of("nome", "Teste")).put(path).then().statusCode(401);
        anonimo.request().delete(path).then().statusCode(401);
        ana.request().get(path).then().body("nome", is("Carteira"));
    }

    @Test
    void preservaHistoricoAoRecusarExclusaoDeContaMovimentada() {
        Client ana = login("ana@exemplo.com");
        long id = conta(ana, "Carteira"), categoria = categoria(ana, "Geral");
        lancar(ana, id, categoria, "RECEITA", "10.00").then().statusCode(201);
        ana.request().delete("/api/contas/" + id).then().statusCode(409)
                .body("mensagem", containsString("lançamentos"));
        ana.request().body(Map.of("nome", "Banco")).put("/api/contas/" + id).then().statusCode(200);
        ana.request().get("/api/saldos").then().body("saldoGeral", is(10.0f)).body("contas[0].nome", is("Banco"));
        ana.request().get("/api/transacoes").then().body("size()", is(1));
    }

    @Test
    void permitePreflightDoAngularParaEditarEExcluir() {
        for (String metodo : new String[]{"PUT", "DELETE"}) {
            given().header("Origin", "http://localhost:4200")
                    .header("Access-Control-Request-Method", metodo)
                    .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
                    .options("/api/contas/1").then().statusCode(200)
                    .header("Access-Control-Allow-Methods", containsString(metodo));
        }
    }

    @Test
    void recusaExclusaoQuandoLancamentoConcorrenteEstaSendoConfirmado() throws Exception {
        Client ana = login("ana@exemplo.com");
        long id = conta(ana, "Carteira"), categoria = categoria(ana, "Geral");
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var insert = connection.prepareStatement("""
                    INSERT INTO transacoes(usuario_id, conta_id, categoria_id, tipo, valor, data)
                    SELECT usuario_id, id, ?, 'RECEITA', 10.00, CURRENT_DATE FROM contas WHERE id = ?
                    """)) {
                insert.setLong(1, categoria);
                insert.setLong(2, id);
                insert.executeUpdate();
            }
            var exclusao = CompletableFuture.supplyAsync(() -> ana.request().delete("/api/contas/" + id));
            try {
                boolean aguardando = false;
                long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (!aguardando && System.nanoTime() < limite) {
                    try (var observador = dataSource.getConnection();
                         var consulta = observador.createStatement();
                         var resultado = consulta.executeQuery("""
                                 SELECT EXISTS(SELECT 1 FROM pg_stat_activity
                                 WHERE datname = current_database() AND wait_event_type = 'Lock'
                                 AND query ILIKE '%contas%')
                                 """)) {
                        resultado.next();
                        aguardando = resultado.getBoolean(1);
                    }
                    if (!aguardando) Thread.sleep(20);
                }
                assertTrue(aguardando, "A exclusão deve aguardar o lançamento concorrente.");
            } finally {
                connection.commit();
            }
            assertEquals(409, exclusao.get(10, TimeUnit.SECONDS).statusCode());
        }
        ana.request().get("/api/transacoes").then().body("size()", is(1));
        ana.request().get("/api/saldos").then().body("saldoGeral", is(10.0f));
    }
}
