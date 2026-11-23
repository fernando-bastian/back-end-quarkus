package br.com.fernandobastian.financeiro.api;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(1)
public class ValidacaoErroMapper implements ExceptionMapper<ResteasyReactiveViolationException> {
    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        return new ErroMapper().toResponse(exception);
    }
}
