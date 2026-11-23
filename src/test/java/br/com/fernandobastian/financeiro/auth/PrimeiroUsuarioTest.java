package br.com.fernandobastian.financeiro.auth;

import br.com.fernandobastian.financeiro.TestEnvironment;
import br.com.fernandobastian.financeiro.TestDatabaseProfile;
import br.com.fernandobastian.financeiro.model.Usuario;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(TestEnvironment.class)
@TestProfile(PrimeiroUsuarioTest.Perfil.class)
class PrimeiroUsuarioTest {
    public static class Perfil extends TestDatabaseProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(super.getConfigOverrides());
            config.putAll(Map.of("app.bootstrap.nome", "Primeiro Usuário", "app.bootstrap.email", "inicial@exemplo.com",
                    "app.bootstrap.senha", "Senha-inicial-segura-123"));
            return config;
        }
    }

    @Inject PrimeiroUsuario bootstrap;
    @Inject Senhas senhas;

    @Test
    @TestTransaction
    void criaUsuarioComHashENaoRedefineSenhaExistente() {
        Usuario usuario = Usuario.find("email", "inicial@exemplo.com").firstResult();
        assertNotNull(usuario);
        assertEquals("Primeiro Usuário", usuario.nome);
        assertTrue(senhas.conferir("Senha-inicial-segura-123", usuario.senhaHash));
        String original = usuario.senhaHash;
        bootstrap.iniciar(null);
        assertEquals(1, Usuario.count("email", "inicial@exemplo.com"));
        assertEquals(original, usuario.senhaHash);
    }
}
