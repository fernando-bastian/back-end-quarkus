package br.com.fernandobastian.financeiro.auth;

import br.com.fernandobastian.financeiro.model.Usuario;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class UsuarioAtual {
    @Inject JsonWebToken jwt;

    public Usuario obter() {
        try {
            Usuario usuario = Usuario.findById(Long.parseLong(jwt.getSubject()));
            if (usuario == null) throw new UnauthorizedException();
            return usuario;
        } catch (NumberFormatException e) {
            throw new UnauthorizedException();
        }
    }
}
