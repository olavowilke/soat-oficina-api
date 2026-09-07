package br.com.oficina.ordemservico.controllers;

import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.presenters.AbrirOrdemServicoResponse;
import br.com.oficina.ordemservico.presenters.OrdemServicoPresenter;
import br.com.oficina.ordemservico.presenters.OrdemServicoResponse;
import br.com.oficina.ordemservico.presenters.OrdemServicoStatusResponse;
import br.com.oficina.ordemservico.presenters.StatusOSPresenter;
import br.com.oficina.ordemservico.presenters.TempoMedioExecucaoResponse;
import br.com.oficina.ordemservico.usecases.AbrirOrdemServicoCommand;
import br.com.oficina.ordemservico.usecases.AbrirOrdemServicoUseCase;
import br.com.oficina.ordemservico.usecases.AdicionarPecaCommand;
import br.com.oficina.ordemservico.usecases.AdicionarPecaUseCase;
import br.com.oficina.ordemservico.usecases.AdicionarServicoCommand;
import br.com.oficina.ordemservico.usecases.AdicionarServicoUseCase;
import br.com.oficina.ordemservico.usecases.AprovarOrcamentoUseCase;
import br.com.oficina.ordemservico.usecases.AvancarStatusUseCase;
import br.com.oficina.ordemservico.usecases.BuscarOrdemServicoUseCase;
import br.com.oficina.ordemservico.usecases.CriarOrdemServicoCommand;
import br.com.oficina.ordemservico.usecases.CriarOrdemServicoUseCase;
import br.com.oficina.ordemservico.usecases.ItemPecaInput;
import br.com.oficina.ordemservico.usecases.ListarOrdensServicoUseCase;
import br.com.oficina.ordemservico.usecases.MonitorarTempoMedioUseCase;
import br.com.oficina.ordemservico.usecases.RecusarOrcamentoUseCase;
import br.com.oficina.ordemservico.usecases.RemoverPecaUseCase;
import br.com.oficina.ordemservico.usecases.RemoverServicoUseCase;
import br.com.oficina.shared.domain.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Ordens de Serviço", description = "Criação e gestão do ciclo de vida das ordens de serviço")
public class OrdemServicoController {

    private final CriarOrdemServicoUseCase criarOrdemServico;
    private final AbrirOrdemServicoUseCase abrirOrdemServico;
    private final AdicionarServicoUseCase adicionarServico;
    private final RemoverServicoUseCase removerServico;
    private final AdicionarPecaUseCase adicionarPeca;
    private final RemoverPecaUseCase removerPeca;
    private final AvancarStatusUseCase avancarStatus;
    private final AprovarOrcamentoUseCase aprovarOrcamento;
    private final RecusarOrcamentoUseCase recusarOrcamento;
    private final BuscarOrdemServicoUseCase buscarOrdemServico;
    private final ListarOrdensServicoUseCase listarOrdensServico;
    private final MonitorarTempoMedioUseCase monitorarTempoMedio;
    private final OrdemServicoPresenter presenter;
    private final StatusOSPresenter statusPresenter;

    public OrdemServicoController(CriarOrdemServicoUseCase criarOrdemServico,
                                  AbrirOrdemServicoUseCase abrirOrdemServico,
                                  AdicionarServicoUseCase adicionarServico,
                                  RemoverServicoUseCase removerServico,
                                  AdicionarPecaUseCase adicionarPeca,
                                  RemoverPecaUseCase removerPeca,
                                  AvancarStatusUseCase avancarStatus,
                                  AprovarOrcamentoUseCase aprovarOrcamento,
                                  RecusarOrcamentoUseCase recusarOrcamento,
                                  BuscarOrdemServicoUseCase buscarOrdemServico,
                                  ListarOrdensServicoUseCase listarOrdensServico,
                                  MonitorarTempoMedioUseCase monitorarTempoMedio,
                                  OrdemServicoPresenter presenter,
                                  StatusOSPresenter statusPresenter) {
        this.criarOrdemServico = criarOrdemServico;
        this.abrirOrdemServico = abrirOrdemServico;
        this.adicionarServico = adicionarServico;
        this.removerServico = removerServico;
        this.adicionarPeca = adicionarPeca;
        this.removerPeca = removerPeca;
        this.avancarStatus = avancarStatus;
        this.aprovarOrcamento = aprovarOrcamento;
        this.recusarOrcamento = recusarOrcamento;
        this.buscarOrdemServico = buscarOrdemServico;
        this.listarOrdensServico = listarOrdensServico;
        this.monitorarTempoMedio = monitorarTempoMedio;
        this.presenter = presenter;
        this.statusPresenter = statusPresenter;
    }

    @Operation(summary = "Criar OS", description = "Cria uma nova ordem de serviço com status RECEBIDA")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "OS criada",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ordens-servico")
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody CriarOrdemServicoRequest request) {
        CriarOrdemServicoCommand command = new CriarOrdemServicoCommand(
                request.clienteId(), request.veiculoId(), request.observacoes());
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.present(criarOrdemServico.execute(command)));
    }

    @Operation(summary = "Abrir OS consolidada",
            description = """
                    Abre uma ordem de serviço completa numa única requisição: cliente e veículo
                    (por ID já cadastrado), mais as listas de serviços e peças. Reserva o estoque
                    das peças, calcula o orçamento e retorna a identificação única da OS criada.
                    A operação é transacional — se qualquer item falhar (ex.: estoque insuficiente),
                    nada é persistido.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "OS aberta",
                    content = @Content(schema = @Schema(implementation = AbrirOrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Cliente, veículo, serviço ou peça não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ordens-servico/abertura")
    public ResponseEntity<AbrirOrdemServicoResponse> abrir(@Valid @RequestBody AbrirOrdemServicoRequest request) {
        List<ItemPecaInput> pecas = (request.pecas() == null ? List.<AdicionarPecaRequest>of() : request.pecas())
                .stream()
                .map(p -> new ItemPecaInput(p.pecaId(), p.quantidade()))
                .toList();
        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                request.clienteId(), request.veiculoId(), request.observacoes(), request.servicoIds(), pecas);
        AbrirOrdemServicoResponse response = presenter.presentAbertura(abrirOrdemServico.execute(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar OSs",
            description = "Lista todas as ordens. Filtra por `clienteId` ou `status` via query params")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de OSs",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/ordens-servico")
    public ResponseEntity<List<OrdemServicoResponse>> listar(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) StatusOS status) {
        return ResponseEntity.ok(presenter.present(listarOrdensServico.execute(clienteId, status)));
    }

    @Operation(summary = "Buscar OS por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OS encontrada",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/ordens-servico/{id}")
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(presenter.present(buscarOrdemServico.execute(id)));
    }

    @Operation(summary = "Consultar status da OS (público)",
            description = "Endpoint público — não requer autenticação. Retorna apenas id e status, sem dados internos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status da OS",
                    content = @Content(schema = @Schema(implementation = OrdemServicoStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirements
    @GetMapping("/public/ordens-servico/{id}/status")
    public ResponseEntity<OrdemServicoStatusResponse> consultarStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(statusPresenter.present(buscarOrdemServico.execute(id)));
    }

    @Operation(summary = "Adicionar serviço à OS",
            description = "Vincula um serviço do catálogo à OS. O preço é fixado no momento da adição")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço adicionado",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "409", description = "OS não permite edição no status atual",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS ou serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ordens-servico/{id}/servicos")
    public ResponseEntity<OrdemServicoResponse> adicionarServico(@PathVariable UUID id,
                                                                  @Valid @RequestBody AdicionarServicoRequest request) {
        AdicionarServicoCommand command = new AdicionarServicoCommand(id, request.servicoId());
        return ResponseEntity.ok(presenter.present(adicionarServico.execute(command)));
    }

    @Operation(summary = "Remover serviço da OS")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço removido",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Item não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "OS não editável no status atual",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/ordens-servico/{id}/servicos/{itemServicoId}")
    public ResponseEntity<OrdemServicoResponse> removerServico(@PathVariable UUID id,
                                                                @PathVariable UUID itemServicoId) {
        return ResponseEntity.ok(presenter.present(removerServico.execute(id, itemServicoId)));
    }

    @Operation(summary = "Adicionar peça à OS",
            description = "Vincula uma peça à OS e desconta do estoque automaticamente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Peça adicionada",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "OS não editável no status atual",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS ou peça não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ordens-servico/{id}/pecas")
    public ResponseEntity<OrdemServicoResponse> adicionarPeca(@PathVariable UUID id,
                                                               @Valid @RequestBody AdicionarPecaRequest request) {
        AdicionarPecaCommand command = new AdicionarPecaCommand(id, request.pecaId(), request.quantidade());
        return ResponseEntity.ok(presenter.present(adicionarPeca.execute(command)));
    }

    @Operation(summary = "Remover peça da OS",
            description = "Remove a peça da OS e devolve a quantidade ao estoque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Peça removida",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Item não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "OS não editável no status atual",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/ordens-servico/{id}/pecas/{itemPecaId}")
    public ResponseEntity<OrdemServicoResponse> removerPeca(@PathVariable UUID id,
                                                             @PathVariable UUID itemPecaId) {
        return ResponseEntity.ok(presenter.present(removerPeca.execute(id, itemPecaId)));
    }

    @Operation(summary = "Avançar status da OS",
            description = """
                    Transições permitidas:
                    - RECEBIDA → EM_DIAGNOSTICO
                    - EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
                    - AGUARDANDO_APROVACAO → EM_EXECUCAO ou CANCELADA
                    - EM_EXECUCAO → FINALIZADA
                    - FINALIZADA → ENTREGUE
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status avançado",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Transição de status inválida",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/ordens-servico/{id}/status")
    public ResponseEntity<OrdemServicoResponse> avancarStatus(@PathVariable UUID id,
                                                               @Valid @RequestBody AvancarStatusRequest request) {
        return ResponseEntity.ok(presenter.present(avancarStatus.execute(id, request.novoStatus())));
    }

    @Operation(summary = "Aprovar orçamento",
            description = "Aprova o orçamento da OS. Requer status AGUARDANDO_APROVACAO → muda para EM_EXECUCAO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento aprovado",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "OS não está aguardando aprovação",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ordens-servico/{id}/aprovar-orcamento")
    public ResponseEntity<OrdemServicoResponse> aprovarOrcamento(@PathVariable UUID id) {
        return ResponseEntity.ok(presenter.present(aprovarOrcamento.execute(id)));
    }

    @Operation(summary = "Recusar orçamento",
            description = "Recusa o orçamento da OS. Requer status AGUARDANDO_APROVACAO → muda para CANCELADA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento recusado, OS cancelada",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "OS não está aguardando aprovação",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ordens-servico/{id}/recusar-orcamento")
    public ResponseEntity<OrdemServicoResponse> recusarOrcamento(@PathVariable UUID id) {
        return ResponseEntity.ok(presenter.present(recusarOrcamento.execute(id)));
    }

    @Operation(summary = "Tempo médio de execução",
            description = "Retorna o tempo médio (em minutos) de execução das OSs finalizadas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tempo médio calculado",
                    content = @Content(schema = @Schema(implementation = TempoMedioExecucaoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/ordens-servico/monitoramento/tempo-medio-execucao")
    public ResponseEntity<TempoMedioExecucaoResponse> monitorarTempoMedioExecucao() {
        return ResponseEntity.ok(presenter.presentTempoMedio(monitorarTempoMedio.execute()));
    }
}
