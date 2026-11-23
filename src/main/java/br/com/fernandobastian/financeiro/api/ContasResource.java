package br.com.fernandobastian.financeiro.api;

import br.com.fernandobastian.financeiro.service.FinanceiroService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
import static br.com.fernandobastian.financeiro.api.FinanceiroDtos.*;

@Path("/api/contas")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContasResource {
    @Inject FinanceiroService financeiro;

    @POST
    public Response criar(@NotNull @Valid NomeEntrada request) {
        return Response.status(201).entity(financeiro.criarConta(request)).build();
    }

    @GET
    public List<CadastroResposta> listar() {
        return financeiro.contas();
    }

    @GET
    @Path("/{id}")
    public CadastroResposta detalhar(@PathParam("id") @Positive long id) {
        return financeiro.detalharConta(id);
    }

    @PUT
    @Path("/{id}")
    public CadastroResposta atualizar(@PathParam("id") @Positive long id, @NotNull @Valid NomeEntrada request) {
        return financeiro.atualizarConta(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") @Positive long id) {
        financeiro.excluirConta(id);
        return Response.noContent().build();
    }
}
