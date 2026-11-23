package br.com.fernandobastian.financeiro.api;

import br.com.fernandobastian.financeiro.service.FinanceiroService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import static br.com.fernandobastian.financeiro.api.FinanceiroDtos.*;

@Path("/api/transferencias")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransferenciasResource {
    @Inject FinanceiroService financeiro;

    @POST
    public Response criar(@NotNull @Valid TransferenciaEntrada request) {
        return Response.status(201).entity(financeiro.transferir(request)).build();
    }
}
