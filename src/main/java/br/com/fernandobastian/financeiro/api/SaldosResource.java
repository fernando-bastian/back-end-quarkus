package br.com.fernandobastian.financeiro.api;

import br.com.fernandobastian.financeiro.service.FinanceiroService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import static br.com.fernandobastian.financeiro.api.FinanceiroDtos.*;

@Path("/api/saldos")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class SaldosResource {
    @Inject FinanceiroService financeiro;

    @GET
    public SaldosResposta consultar() {
        return financeiro.saldos();
    }
}
