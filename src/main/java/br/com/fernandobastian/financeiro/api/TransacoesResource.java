package br.com.fernandobastian.financeiro.api;

import br.com.fernandobastian.financeiro.service.FinanceiroService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
import static br.com.fernandobastian.financeiro.api.FinanceiroDtos.*;

@Path("/api/transacoes")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransacoesResource {
    @Inject FinanceiroService financeiro;

    @POST
    public Response criar(@NotNull @Valid TransacaoEntrada request) {
        return Response.status(201).entity(financeiro.lancar(request)).build();
    }

    @GET
    public List<TransacaoResposta> listar(@QueryParam("contaId") String contaId,
                                         @QueryParam("categoriaId") String categoriaId) {
        return financeiro.transacoes(id(contaId), id(categoriaId));
    }

    private Long id(String value) {
        if (value == null) return null;
        try {
            long id = Long.parseLong(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
        }
        throw new ApiException(400, "Os filtros contaId e categoriaId devem ser inteiros positivos.");
    }
}
