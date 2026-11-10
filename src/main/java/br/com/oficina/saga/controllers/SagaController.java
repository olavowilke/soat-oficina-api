package br.com.oficina.saga.controllers;

import br.com.oficina.saga.presenters.SagaPresenter;
import br.com.oficina.saga.presenters.SagaResponse;
import br.com.oficina.saga.usecases.IniciarSagaUseCase;
import br.com.oficina.saga.usecases.ListarSagasDaOsUseCase;
import br.com.oficina.shared.domain.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Saga da OS", description = "Orçamento, pagamento e execução da ordem de serviço")
public class SagaController {

    private final IniciarSagaUseCase iniciarSaga;
    private final ListarSagasDaOsUseCase listarSagas;
    private final SagaPresenter presenter;

    public SagaController(IniciarSagaUseCase iniciarSaga, ListarSagasDaOsUseCase listarSagas,
                          SagaPresenter presenter) {
        this.iniciarSaga = iniciarSaga;
        this.listarSagas = listarSagas;
        this.presenter = presenter;
    }

    @Operation(summary = "Enviar OS para orçamento",
            description = "Move a OS para AGUARDANDO_ORCAMENTO e inicia a saga. O orçamento é gerado de forma assíncrona.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Saga iniciada",
                    content = @Content(schema = @Schema(implementation = SagaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "OS sem itens, em status inválido ou com saga ativa",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ordens-servico/{id}/enviar-orcamento")
    public ResponseEntity<SagaResponse> enviarOrcamento(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(presenter.present(iniciarSaga.execute(id)));
    }

    @Operation(summary = "Listar sagas da OS", description = "Todas as sagas da OS, da mais nova para a mais antiga")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sagas da OS",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SagaResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/ordens-servico/{id}/saga")
    public ResponseEntity<List<SagaResponse>> listar(@PathVariable UUID id) {
        return ResponseEntity.ok(presenter.present(listarSagas.execute(id)));
    }
}
