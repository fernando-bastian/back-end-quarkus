package br.com.fernandobastian.financeiro.auth;

import br.com.fernandobastian.financeiro.api.ApiException;
import br.com.fernandobastian.financeiro.model.Usuario;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AuthService {
    @Inject Senhas senhas;
    @ConfigProperty(name = "app.jwt.duration-seconds") long duration;
    private String hashFicticio;

    void prepararLogin(@Observes StartupEvent evento) {
        hashFicticio = senhas.hash("valor-interno-sem-usuario");
    }

    public Usuario autenticar(String email, String senha) {
        Usuario usuario = Usuario.find("email", normalizarEmail(email)).firstResult();
        // Verifica um hash mesmo quando o e-mail não existe para manter o custo do login semelhante.
        boolean valido = senhas.conferir(senha, usuario == null ? hashFicticio : usuario.senhaHash);
        if (usuario == null || !valido) throw new ApiException(401, "E-mail ou senha inválidos.");
        return usuario;
    }

    public String token(Usuario usuario) {
        Instant now = Instant.now();
        return Jwt.issuer("gestao-financeira").subject(usuario.id.toString()).groups("usuario")
                .issuedAt(now).expiresAt(now.plusSeconds(duration)).sign();
    }

    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
