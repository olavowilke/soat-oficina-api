package br.com.oficina.servico.controllers;

import br.com.oficina.servico.presenters.ServicoPresenter;
import br.com.oficina.servico.presenters.ServicoResponse;
import br.com.oficina.servico.usecases.AtualizarServicoCommand;
import br.com.oficina.servico.usecases.AtualizarServicoUseCase;
import br.com.oficina.servico.usecases.BuscarServicoUseCase;
import br.com.oficina.servico.usecases.CadastrarServicoCommand;
import br.com.oficina.servico.usecases.CadastrarServicoUseCase;
import br.com.oficina.servico.usecases.ListarServicosUseCase;
import br.com.oficina.servico.usecases.RemoverServicoUseCase;
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
@RequestMapping("/servicos")
@Tag(name = "Catálogo de Serviços", description = "Gestão do catálogo de serviços da oficina")
public class ServicoController {

    private final CadastrarServicoUseCase cadastrarServico;
    private final AtualizarServicoUseCase atualizarServico;
    private final BuscarServicoUseCase buscarServico;
    private final ListarServicosUseCase listarServicos;
    private final RemoverServicoUseCase removerServico;
    private final ServicoPresenter presenter;

    public ServicoController(CadastrarServicoUseCase cadastrarServico,
                             AtualizarServicoUseCase atualizarServico,
                             BuscarServicoUseCase buscarServico,
                             ListarServicosUseCase listarServicos,
                             RemoverServicoUseCase removerServico,
                             ServicoPresenter presenter) {
        this.cadastrarServico = cadastrarServico;
        this.atualizarServico = atualizarServico;
        this.buscarServico = buscarServico;
        this.listarServicos = listarServicos;
        this.removerServico = removerServico;
        this.presenter = presenter;
    }

    @Operation(summary = "Cadastrar serviço", description = "Adiciona um novo serviço ao catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ServicoResponse> cadastrar(@Valid @RequestBody CadastrarServicoRequest request) {
        CadastrarServicoCommand command = new CadastrarServicoCommand(
                request.nome(), request.descricao(), request.preco(), request.tempoEstimadoMinutos());
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.present(cadastrarServico.execute(command)));
    }

    @Operation(summary = "Listar serviços", description = "Retorna todos os serviços ativos do catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de serviços",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(presenter.present(listarServicos.execute()));
    }

    @Operation(summary = "Buscar serviço por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ServicoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(presenter.present(buscarServico.execute(id)));
    }

    @Operation(summary = "Atualizar serviço", description = "Atualiza nome, descrição, preço e tempo estimado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ServicoResponse> atualizar(@PathVariable UUID id,
                                                     @Valid @RequestBody AtualizarServicoRequest request) {
        AtualizarServicoCommand command = new AtualizarServicoCommand(
                request.nome(), request.descricao(), request.preco(), request.tempoEstimadoMinutos());
        return ResponseEntity.ok(presenter.present(atualizarServico.execute(id, command)));
    }

    @Operation(summary = "Remover serviço", description = "Desativa o serviço do catálogo (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço removido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        removerServico.execute(id);
        return ResponseEntity.noContent().build();
    }
}
