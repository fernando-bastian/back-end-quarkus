package br.com.fernandobastian.financeiro.auth;

import br.com.fernandobastian.financeiro.model.Usuario;
import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject AuthService auth;
    @Inject UsuarioAtual atual;
    @Inject CsrfTokenParameterProvider csrf;
    @ConfigProperty(name = "app.cookie.secure") boolean secure;
    @ConfigProperty(name = "app.jwt.duration-seconds") int duration;

    public record Login(@NotBlank @Size(max = 256) String email,
                        @NotBlank @Size(max = 128) String senha) {}
    public record UsuarioResposta(Long id, String nome, String email) {
        static UsuarioResposta de(Usuario usuario) {
            return new UsuarioResposta(usuario.id, usuario.nome, usuario.email);
        }
    }
    public record CsrfResposta(String token) {}

    @GET
    @Path("/csrf")
    public CsrfResposta csrf() {
        return new CsrfResposta(csrf.getToken());
    }

    @POST
    @Path("/login")
    public Response login(@NotNull @Valid Login request) {
        Usuario usuario = auth.autenticar(request.email(), request.senha());
        return Response.ok(UsuarioResposta.de(usuario)).cookie(cookie(auth.token(usuario), duration)).build();
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    @Authenticated
    public Response logout() {
        return Response.noContent().cookie(cookie("", 0)).build();
    }

    @GET
    @Path("/me")
    @Authenticated
    public UsuarioResposta me() {
        return UsuarioResposta.de(atual.obter());
    }

    private NewCookie cookie(String value, int maxAge) {
        return new NewCookie.Builder("AUTH_TOKEN").value(value).path("/api").httpOnly(true)
                .secure(secure).sameSite(NewCookie.SameSite.LAX).maxAge(maxAge).build();
    }
}
