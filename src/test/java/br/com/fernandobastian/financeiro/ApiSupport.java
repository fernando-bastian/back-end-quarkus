package br.com.fernandobastian.financeiro;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import static io.restassured.RestAssured.given;

abstract class ApiSupport {
    @Inject DataSource dataSource;
    private static final String HASH = fixturePassword();

    @BeforeEach
    void prepararUsuarios() throws Exception {
        sql("DROP TRIGGER IF EXISTS falha_transferencia ON transacoes");
        sql("TRUNCATE transacoes, categorias, contas, usuarios RESTART IDENTITY CASCADE");
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("INSERT INTO usuarios (nome,email,senha_hash) VALUES (?,?,?)")) {
            for (String name : new String[]{"Ana", "Bruno"}) {
                statement.setString(1, name);
                statement.setString(2, name.toLowerCase() + "@exemplo.com");
                statement.setString(3, HASH);
                statement.executeUpdate();
            }
        }
    }

    void sql(String command) throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(command);
        }
    }

    Client csrf() {
        Response response = given().get("/api/auth/csrf").then().statusCode(200).extract().response();
        return new Client(new HashMap<>(response.cookies()), response.path("token"));
    }

    Client login(String email) {
        Client client = csrf();
        Response response = client.request().body(Map.of("email", email, "senha", "Senha-segura-123"))
                .post("/api/auth/login").then().statusCode(200).extract().response();
        client.cookies.putAll(response.cookies());
        return client;
    }

    long conta(Client client, String nome) {
        return client.request().body(Map.of("nome", nome)).post("/api/contas")
                .then().statusCode(201).extract().jsonPath().getLong("id");
    }

    long categoria(Client client, String nome) {
        return client.request().body(Map.of("nome", nome)).post("/api/categorias")
                .then().statusCode(201).extract().jsonPath().getLong("id");
    }

    Response lancar(Client client, long contaId, long categoriaId, String tipo, Object valor) {
        return client.request().body(Map.of("contaId", contaId, "categoriaId", categoriaId,
                "tipo", tipo, "valor", valor, "data", "2026-11-16", "descricao", "Teste"))
                .post("/api/transacoes");
    }

    record Client(Map<String, String> cookies, String token) {
        RequestSpecification request() {
            return given().cookies(cookies).header("X-XSRF-TOKEN", token).contentType("application/json");
        }
    }

    private static String fixturePassword() {
        try {
            byte[] salt = "fixture-test-salt".getBytes(StandardCharsets.UTF_8);
            var spec = new PBEKeySpec("Senha-segura-123".toCharArray(), salt, 600_000, 256);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "pbkdf2-sha256$600000$" + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
