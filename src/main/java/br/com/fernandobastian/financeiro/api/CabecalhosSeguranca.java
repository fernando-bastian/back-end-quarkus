package br.com.fernandobastian.financeiro.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;

@Provider
public class CabecalhosSeguranca implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("Cache-Control", "no-store");
        response.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
        // erros dos filtros podem chegar aqui sem corpo
        if (response.getStatus() >= 400 && !response.hasEntity()) {
            String message = switch (response.getStatus()) {
                case 400 -> "Requisição inválida. Confira o JSON, os parâmetros e o token CSRF.";
                case 401 -> "Autenticação necessária.";
                case 403 -> "Acesso não permitido.";
                default -> "Não foi possível processar a requisição.";
            };
            response.setEntity(new Erro(message));
            response.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_JSON);
        }
    }
}
