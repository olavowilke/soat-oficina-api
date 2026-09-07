package br.com.oficina.ordemservico.controllers;

import br.com.oficina.cliente.controllers.CadastrarClienteRequest;
import br.com.oficina.cliente.entities.TipoDocumento;
import br.com.oficina.cliente.presenters.ClienteResponse;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.presenters.AbrirOrdemServicoResponse;
import br.com.oficina.ordemservico.presenters.OrdemServicoResponse;
import br.com.oficina.ordemservico.presenters.OrdemServicoStatusResponse;
import br.com.oficina.ordemservico.presenters.TempoMedioExecucaoResponse;
import br.com.oficina.peca.controllers.CadastrarPecaRequest;
import br.com.oficina.peca.presenters.PecaResponse;
import br.com.oficina.servico.controllers.CadastrarServicoRequest;
import br.com.oficina.servico.presenters.ServicoResponse;
import br.com.oficina.shared.BaseControllerIT;
import br.com.oficina.veiculo.controllers.CadastrarVeiculoRequest;
import br.com.oficina.veiculo.presenters.VeiculoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoControllerIT extends BaseControllerIT {

    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;
    private UUID pecaId;

    @BeforeEach
    void setUp() {
        CadastrarClienteRequest clienteReq = new CadastrarClienteRequest(
                "João Silva", "joao@email.com", "11999999999", "529.982.247-25", TipoDocumento.CPF);
        clienteId = authPost("/clientes", clienteReq, ClienteResponse.class).getBody().id();

        CadastrarVeiculoRequest veiculoReq = new CadastrarVeiculoRequest(
                clienteId, "ABC-1234", "Toyota", "Corolla", 2020, "Prata");
        veiculoId = authPost("/veiculos", veiculoReq, VeiculoResponse.class).getBody().id();

        CadastrarServicoRequest servicoReq = new CadastrarServicoRequest(
                "Troca de óleo", "Óleo 5W30", new BigDecimal("150.00"), 30);
        servicoId = authPost("/servicos", servicoReq, ServicoResponse.class).getBody().id();

        CadastrarPecaRequest pecaReq = new CadastrarPecaRequest(
                "Filtro de Óleo", "Filtro original", new BigDecimal("35.00"), 20);
        pecaId = authPost("/pecas", pecaReq, PecaResponse.class).getBody().id();
    }

    private OrdemServicoResponse criarOS() {
        CriarOrdemServicoRequest request = new CriarOrdemServicoRequest(clienteId, veiculoId, "Revisão completa");
        ResponseEntity<OrdemServicoResponse> response = authPost("/ordens-servico", request, OrdemServicoResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    @Test
    void shouldReturn201WhenCreatingOS() {
        OrdemServicoResponse os = criarOS();
        assertNotNull(os);
        assertEquals(StatusOS.RECEBIDA, os.status());
        assertEquals(clienteId, os.clienteId());
        assertEquals(veiculoId, os.veiculoId());
    }

    @Test
    void shouldReturn200WhenListingOS() {
        criarOS();
        ResponseEntity<OrdemServicoResponse[]> response = authGet("/ordens-servico", OrdemServicoResponse[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void shouldReturn200WhenGettingOSById() {
        OrdemServicoResponse created = criarOS();
        ResponseEntity<OrdemServicoResponse> response = authGet("/ordens-servico/" + created.id(), OrdemServicoResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(created.id(), response.getBody().id());
    }

    @Test
    void shouldReturn200WhenAddingServicoToOS() {
        OrdemServicoResponse os = criarOS();
        AdicionarServicoRequest request = new AdicionarServicoRequest(servicoId);
        ResponseEntity<OrdemServicoResponse> response = authPost(
                "/ordens-servico/" + os.id() + "/servicos", request, OrdemServicoResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().itensServico().size());
        assertEquals(new BigDecimal("150.00"), response.getBody().orcamentoTotal());
    }

    @Test
    void shouldReturn200WhenAddingPecaToOS() {
        OrdemServicoResponse os = criarOS();
        AdicionarPecaRequest request = new AdicionarPecaRequest(pecaId, 2);
        ResponseEntity<OrdemServicoResponse> response = authPost(
                "/ordens-servico/" + os.id() + "/pecas", request, OrdemServicoResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().itensPeca().size());
        assertEquals(new BigDecimal("70.00"), response.getBody().orcamentoTotal());
    }

    @Test
    void shouldExecuteFullFlowFromRecebidaToEntregue() {
        OrdemServicoResponse os = criarOS();
        UUID osId = os.id();

        patchStatus(osId, StatusOS.EM_DIAGNOSTICO);
        patchStatus(osId, StatusOS.AGUARDANDO_APROVACAO);

        ResponseEntity<OrdemServicoResponse> aprovado = authPost(
                "/ordens-servico/" + osId + "/aprovar-orcamento", null, OrdemServicoResponse.class);
        assertEquals(HttpStatus.OK, aprovado.getStatusCode());
        assertEquals(StatusOS.EM_EXECUCAO, aprovado.getBody().status());
        assertNotNull(aprovado.getBody().inicioExecucao());

        OrdemServicoResponse finalizada = patchStatus(osId, StatusOS.FINALIZADA);
        assertEquals(StatusOS.FINALIZADA, finalizada.status());
        assertNotNull(finalizada.fimExecucao());

        OrdemServicoResponse entregue = patchStatus(osId, StatusOS.ENTREGUE);
        assertEquals(StatusOS.ENTREGUE, entregue.status());
    }

    @Test
    void shouldReturn409WhenInvalidStatusTransition() {
        OrdemServicoResponse os = criarOS();
        AvancarStatusRequest request = new AvancarStatusRequest(StatusOS.FINALIZADA);
        ResponseEntity<Object> response = authPatch("/ordens-servico/" + os.id() + "/status",
                request, Object.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenRecusandoOrcamento() {
        OrdemServicoResponse os = criarOS();
        UUID osId = os.id();

        patchStatus(osId, StatusOS.EM_DIAGNOSTICO);
        patchStatus(osId, StatusOS.AGUARDANDO_APROVACAO);

        ResponseEntity<OrdemServicoResponse> recusado = authPost(
                "/ordens-servico/" + osId + "/recusar-orcamento", null, OrdemServicoResponse.class);
        assertEquals(HttpStatus.OK, recusado.getStatusCode());
        assertEquals(StatusOS.CANCELADA, recusado.getBody().status());
    }

    @Test
    void shouldReturn200WhenConsultingPublicStatusWithoutToken() {
        OrdemServicoResponse os = criarOS();
        ResponseEntity<OrdemServicoStatusResponse> response = restTemplate.getForEntity(
                "/public/ordens-servico/" + os.id() + "/status", OrdemServicoStatusResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(os.id(), response.getBody().id());
        assertEquals(os.status(), response.getBody().status());
    }

    @Test
    void shouldReturn401WhenAccessingWithoutToken() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/ordens-servico", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturn200WhenGettingTempoMedioExecucao() {
        ResponseEntity<TempoMedioExecucaoResponse> response = authGet(
                "/ordens-servico/monitoramento/tempo-medio-execucao", TempoMedioExecucaoResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldReturn201AndOsIdWhenOpeningConsolidatedOS() {
        AbrirOrdemServicoRequest request = new AbrirOrdemServicoRequest(
                clienteId, veiculoId, "Abertura consolidada",
                List.of(servicoId),
                List.of(new AdicionarPecaRequest(pecaId, 2)));

        ResponseEntity<AbrirOrdemServicoResponse> response = authPost(
                "/ordens-servico/abertura", request, AbrirOrdemServicoResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().ordemServicoId());
        assertEquals(StatusOS.RECEBIDA, response.getBody().status());
        // 150 (serviço) + 2 * 35 (peças) = 220
        assertEquals(0, new BigDecimal("220.00").compareTo(response.getBody().orcamentoTotal()));

        // a OS criada deve ser recuperável pelo ID retornado, com os itens informados
        ResponseEntity<OrdemServicoResponse> criada = authGet(
                "/ordens-servico/" + response.getBody().ordemServicoId(), OrdemServicoResponse.class);
        assertEquals(HttpStatus.OK, criada.getStatusCode());
        assertEquals(1, criada.getBody().itensServico().size());
        assertEquals(1, criada.getBody().itensPeca().size());
    }

    @Test
    void shouldReturn400WhenOpeningConsolidatedOSWithInsufficientStock() {
        AbrirOrdemServicoRequest request = new AbrirOrdemServicoRequest(
                clienteId, veiculoId, null,
                List.of(),
                List.of(new AdicionarPecaRequest(pecaId, 999)));

        ResponseEntity<Object> response = authPost("/ordens-servico/abertura", request, Object.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn422WhenOpeningConsolidatedOSWithoutCliente() {
        AbrirOrdemServicoRequest request = new AbrirOrdemServicoRequest(
                null, veiculoId, null, List.of(), List.of());

        ResponseEntity<Object> response = authPost("/ordens-servico/abertura", request, Object.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    private OrdemServicoResponse patchStatus(UUID osId, StatusOS status) {
        AvancarStatusRequest request = new AvancarStatusRequest(status);
        ResponseEntity<OrdemServicoResponse> response = authPatch(
                "/ordens-servico/" + osId + "/status", request, OrdemServicoResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }
}
