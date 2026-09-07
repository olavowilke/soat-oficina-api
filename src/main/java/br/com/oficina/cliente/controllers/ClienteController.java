package br.com.oficina.cliente.controllers;

import br.com.oficina.cliente.presenters.ClientePresenter;
import br.com.oficina.cliente.presenters.ClienteResponse;
import br.com.oficina.cliente.usecases.AtualizarClienteCommand;
import br.com.oficina.cliente.usecases.AtualizarClienteUseCase;
import br.com.oficina.cliente.usecases.BuscarClienteUseCase;
import br.com.oficina.cliente.usecases.CadastrarClienteCommand;
import br.com.oficina.cliente.usecases.CadastrarClienteUseCase;
import br.com.oficina.cliente.usecases.ListarClientesUseCase;
import br.com.oficina.cliente.usecases.RemoverClienteUseCase;
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
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Cadastro e gestão de clientes")
public class ClienteController {

    private final CadastrarClienteUseCase cadastrarCliente;
    private final AtualizarClienteUseCase atualizarCliente;
    private final BuscarClienteUseCase buscarCliente;
    private final ListarClientesUseCase listarClientes;
    private final RemoverClienteUseCase removerCliente;
    private final ClientePresenter presenter;

    public ClienteController(CadastrarClienteUseCase cadastrarCliente,
                             AtualizarClienteUseCase atualizarCliente,
                             BuscarClienteUseCase buscarCliente,
                             ListarClientesUseCase listarClientes,
                             RemoverClienteUseCase removerCliente,
                             ClientePresenter presenter) {
        this.cadastrarCliente = cadastrarCliente;
        this.atualizarCliente = atualizarCliente;
        this.buscarCliente = buscarCliente;
        this.listarClientes = listarClientes;
        this.removerCliente = removerCliente;
        this.presenter = presenter;
    }

    @Operation(summary = "Cadastrar cliente", description = "Cria um novo cliente com CPF ou CNPJ validado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Documento inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody CadastrarClienteRequest request) {
        CadastrarClienteCommand command = new CadastrarClienteCommand(
                request.nome(), request.email(), request.telefone(),
                request.documento(), request.tipoDocumento());
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.present(cadastrarCliente.execute(command)));
    }

    @Operation(summary = "Listar clientes", description = "Retorna todos os clientes ativos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(presenter.present(listarClientes.execute()));
    }

    @Operation(summary = "Buscar cliente por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(presenter.present(buscarCliente.execute(id)));
    }

    @Operation(summary = "Atualizar cliente", description = "Atualiza nome, email e telefone")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente atualizado",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable UUID id,
                                                     @Valid @RequestBody AtualizarClienteRequest request) {
        AtualizarClienteCommand command = new AtualizarClienteCommand(
                request.nome(), request.email(), request.telefone());
        return ResponseEntity.ok(presenter.present(atualizarCliente.execute(id, command)));
    }

    @Operation(summary = "Remover cliente", description = "Desativa o cliente (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente removido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        removerCliente.execute(id);
        return ResponseEntity.noContent().build();
    }
}
