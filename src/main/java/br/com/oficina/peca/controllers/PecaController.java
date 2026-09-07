package br.com.oficina.peca.controllers;

import br.com.oficina.peca.presenters.PecaPresenter;
import br.com.oficina.peca.presenters.PecaResponse;
import br.com.oficina.peca.usecases.AjustarEstoquePecaUseCase;
import br.com.oficina.peca.usecases.AtualizarPecaCommand;
import br.com.oficina.peca.usecases.AtualizarPecaUseCase;
import br.com.oficina.peca.usecases.BuscarPecaUseCase;
import br.com.oficina.peca.usecases.CadastrarPecaCommand;
import br.com.oficina.peca.usecases.CadastrarPecaUseCase;
import br.com.oficina.peca.usecases.ListarPecasUseCase;
import br.com.oficina.peca.usecases.RemoverPecaUseCase;
import br.com.oficina.shared.domain.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pecas")
@Tag(name = "Peças e Insumos", description = "Gestão do estoque de peças e insumos")
public class PecaController {

    private final CadastrarPecaUseCase cadastrarPeca;
    private final AtualizarPecaUseCase atualizarPeca;
    private final BuscarPecaUseCase buscarPeca;
    private final ListarPecasUseCase listarPecas;
    private final AjustarEstoquePecaUseCase ajustarEstoquePeca;
    private final RemoverPecaUseCase removerPeca;
    private final PecaPresenter presenter;

    public PecaController(CadastrarPecaUseCase cadastrarPeca,
                          AtualizarPecaUseCase atualizarPeca,
                          BuscarPecaUseCase buscarPeca,
                          ListarPecasUseCase listarPecas,
                          AjustarEstoquePecaUseCase ajustarEstoquePeca,
                          RemoverPecaUseCase removerPeca,
                          PecaPresenter presenter) {
        this.cadastrarPeca = cadastrarPeca;
        this.atualizarPeca = atualizarPeca;
        this.buscarPeca = buscarPeca;
        this.listarPecas = listarPecas;
        this.ajustarEstoquePeca = ajustarEstoquePeca;
        this.removerPeca = removerPeca;
        this.presenter = presenter;
    }

    @Operation(summary = "Cadastrar peça", description = "Adiciona uma nova peça ao estoque")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Peça criada",
                    content = @Content(schema = @Schema(implementation = PecaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<PecaResponse> cadastrar(@Valid @RequestBody CadastrarPecaRequest request) {
        CadastrarPecaCommand command = new CadastrarPecaCommand(
                request.nome(), request.descricao(), request.precoUnitario(), request.quantidadeEstoqueInicial());
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.present(cadastrarPeca.execute(command)));
    }

    @Operation(summary = "Listar peças", description = "Retorna todas as peças ativas em estoque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de peças",
                    content = @Content(schema = @Schema(implementation = PecaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(presenter.present(listarPecas.execute()));
    }

    @Operation(summary = "Buscar peça por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Peça encontrada",
                    content = @Content(schema = @Schema(implementation = PecaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Peça não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PecaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(presenter.present(buscarPeca.execute(id)));
    }

    @Operation(summary = "Atualizar peça", description = "Atualiza nome, descrição e preço unitário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Peça atualizada",
                    content = @Content(schema = @Schema(implementation = PecaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Peça não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<PecaResponse> atualizar(@PathVariable UUID id,
                                                  @Valid @RequestBody AtualizarPecaRequest request) {
        AtualizarPecaCommand command = new AtualizarPecaCommand(
                request.nome(), request.descricao(), request.precoUnitario());
        return ResponseEntity.ok(presenter.present(atualizarPeca.execute(id, command)));
    }

    @Operation(summary = "Ajustar estoque",
            description = "Ajusta a quantidade em estoque. Use valores positivos para entrada e negativos para saída")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque ajustado",
                    content = @Content(schema = @Schema(implementation = PecaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Peça não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/estoque")
    public ResponseEntity<PecaResponse> ajustarEstoque(@PathVariable UUID id,
                                                       @Valid @RequestBody AjustarEstoqueRequest request) {
        return ResponseEntity.ok(presenter.present(ajustarEstoquePeca.execute(id, request.quantidade())));
    }

    @Operation(summary = "Remover peça", description = "Desativa a peça do estoque (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Peça removida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Peça não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        removerPeca.execute(id);
        return ResponseEntity.noContent().build();
    }
}
