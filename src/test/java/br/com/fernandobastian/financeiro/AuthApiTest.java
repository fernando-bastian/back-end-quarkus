package br.com.fernandobastian.financeiro;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import io.smallrye.jwt.build.Jwt;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(TestEnvironment.class)
@TestProfile(TestDatabaseProfile.class)
class AuthApiTest extends ApiSupport {
    @Test
    void loginEmiteCookieHttpOnlyEConsultaUsuario() {
        Client client = csrf();
        var response = client.request().body(Map.of("email", " ANA@EXEMPLO.COM ", "senha", "Senha-segura-123"))
                .post("/api/auth/login").then().statusCode(200)
                .body("nome", is("Ana")).body("senha", nullValue()).body("senhaHash", nullValue())
                .body("token", nullValue()).extract().response();
        var cookie = response.getDetailedCookie("AUTH_TOKEN");
        assertTrue(cookie.isHttpOnly());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("/api", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge());
        client.cookies().putAll(response.cookies());
        client.request().get("/api/auth/me").then().statusCode(200).body("email", is("ana@exemplo.com"));
    }

    @Test
    void rejeitaSenhaIncorretaEUsuarioInexistenteSemDistinguir() {
        Client client = csrf();
        for (String email : new String[]{"ana@exemplo.com", "ninguem@exemplo.com"}) {
            client.request().body(Map.of("email", email, "senha", "senha-errada"))
                    .post("/api/auth/login").then().statusCode(401)
                    .body("mensagem", is("E-mail ou senha inválidos."));
        }
    }

    @Test
    void logoutRemoveCookieERejeitaClienteSemSessao() {
        Client client = login("ana@exemplo.com");
        var response = given().cookies(client.cookies()).header("X-XSRF-TOKEN", client.token())
                .post("/api/auth/logout").then().statusCode(204).extract().response();
        assertEquals(0, response.getDetailedCookie("AUTH_TOKEN").getMaxAge());
        client.cookies().remove("AUTH_TOKEN");
        client.request().get("/api/auth/me").then().statusCode(401).body("mensagem", not(emptyOrNullString()));
    }

    @Test
    void exigeAutenticacaoEmTodasAsConsultas() {
        for (String path : new String[]{"contas", "categorias", "transacoes", "saldos", "auth/me"}) {
            given().get("/api/" + path).then().statusCode(401);
        }
    }

    @Test
    void exigeCsrfInclusiveNoLoginENoLogout() {
        Client client = csrf();
        given().contentType("application/json").body(Map.of("email", "ana@exemplo.com", "senha", "Senha-segura-123"))
                .post("/api/auth/login").then().statusCode(400).body("mensagem", not(emptyOrNullString()));
        given().cookies(client.cookies()).header("X-XSRF-TOKEN", "invalido")
                .contentType("application/json").body(Map.of("email", "ana@exemplo.com", "senha", "Senha-segura-123"))
                .post("/api/auth/login").then().statusCode(400);
        client = login("ana@exemplo.com");
        given().cookies(client.cookies()).post("/api/auth/logout").then().statusCode(400);
        given().cookies(client.cookies()).contentType("application/json").body(Map.of("nome", "Conta"))
                .post("/api/contas").then().statusCode(400);
    }

    @Test
    void rejeitaJwtAdulteradoExpiradoEUsuarioRemovido() throws Exception {
        given().cookie("AUTH_TOKEN", "invalido").get("/api/auth/me").then().statusCode(401);
        String expired = Jwt.issuer("gestao-financeira").subject("1").groups("usuario")
                .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600)).sign();
        given().cookie("AUTH_TOKEN", expired).get("/api/auth/me").then().statusCode(401);
        Client client = login("ana@exemplo.com");
        sql("DELETE FROM usuarios WHERE email='ana@exemplo.com'");
        client.request().get("/api/contas").then().statusCode(401);
    }

    @Test
    void corsPermiteAngularComCredenciaisERejeitaOutraOrigem() {
        given().header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
                .options("/api/auth/login").then().statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:4200")
                .header("Access-Control-Allow-Credentials", "true");
        given().header("Origin", "https://malicioso.example").header("Access-Control-Request-Method", "POST")
                .options("/api/auth/login").then().statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }
}
