package br.com.fernandobastian.financeiro.auth;

import br.com.fernandobastian.financeiro.model.Usuario;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Validator;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PrimeiroUsuario {
    @Inject Senhas senhas;
    @Inject Validator validator;
    @ConfigProperty(name = "app.bootstrap.nome", defaultValue = "Estudante") String nome;
    @ConfigProperty(name = "app.bootstrap.email") Optional<String> email;
    @ConfigProperty(name = "app.bootstrap.senha") Optional<String> senha;

    private record Dados(@NotBlank @Size(max = 100) String nome,
                         @NotBlank @Email @Size(max = 254) String email,
                         @NotBlank @Size(min = 12, max = 128) String senha) {}

    @Transactional
    void iniciar(@Observes StartupEvent event) {
        if (email.isEmpty() && senha.isEmpty()) return;
        if (email.isEmpty() || senha.isEmpty()) {
            throw new IllegalStateException("Configure APP_BOOTSTRAP_EMAIL e APP_BOOTSTRAP_SENHA juntos.");
        }
        var dados = new Dados(nome.strip(), AuthService.normalizarEmail(email.get()), senha.get());
        if (!validator.validate(dados).isEmpty()) {
            throw new IllegalStateException("Primeiro usuário inválido: nome até 100 caracteres, e-mail válido e senha de 12 a 128 caracteres.");
        }
        if (Usuario.count("email", dados.email()) > 0) return;
        var usuario = new Usuario();
        usuario.nome = dados.nome();
        usuario.email = dados.email();
        usuario.senhaHash = senhas.hash(dados.senha());
        usuario.persist();
    }
}
