package br.com.fernandobastian.financeiro.api;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class ErroMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(ErroMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof ApiException error) {
            return response(error.status, new Erro(error.getMessage()));
        }
        if (exception instanceof ConstraintViolationException error) {
            var details = error.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
            return response(400, new Erro("Dados inválidos.", details));
        }
        if (exception instanceof UnauthorizedException) {
            return response(401, new Erro("Autenticação necessária."));
        }
        if (exception instanceof ForbiddenException) {
            return response(403, new Erro("Acesso não permitido."));
        }
        if (exception instanceof WebApplicationException error) {
            int status = error.getResponse().getStatus();
            String message = switch (status) {
                case 400 -> "Requisição inválida. Confira o JSON, os parâmetros e o token CSRF.";
                case 401 -> "Autenticação necessária.";
                case 403 -> "Acesso não permitido.";
                case 404 -> "Recurso não encontrado.";
                case 405 -> "Método HTTP não permitido.";
                case 415 -> "Envie o conteúdo como application/json.";
                default -> "Não foi possível processar a requisição.";
            };
            return response(status, new Erro(message));
        }
        LOG.error("Falha ao processar requisição", exception);
        return response(500, new Erro("Erro interno ao processar a requisição."));
    }

    static Response response(int status, Erro error) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(error).build();
    }
}
