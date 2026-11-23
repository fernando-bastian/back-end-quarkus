package br.com.fernandobastian.financeiro.api;

import br.com.fernandobastian.financeiro.service.FinanceiroService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
import static br.com.fernandobastian.financeiro.api.FinanceiroDtos.*;

@Path("/api/categorias")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoriasResource {
    @Inject FinanceiroService financeiro;

    @POST
    public Response criar(@NotNull @Valid NomeEntrada request) {
        return Response.status(201).entity(financeiro.criarCategoria(request)).build();
    }

    @GET
    public List<CadastroResposta> listar() {
        return financeiro.categorias();
    }
}
