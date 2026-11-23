package br.com.fernandobastian.financeiro.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JsonErroMapper implements ExceptionMapper<JsonProcessingException> {
    @Override
    public Response toResponse(JsonProcessingException exception) {
        return ErroMapper.response(400, new Erro("JSON inválido. Confira os campos, tipos e datas informados."));
    }
}
