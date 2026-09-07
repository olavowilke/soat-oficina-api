package br.com.oficina.auth.controllers;

import br.com.oficina.auth.entities.Role;
import br.com.oficina.auth.entities.Usuario;
import br.com.oficina.auth.presenters.AuthPresenter;
import br.com.oficina.auth.presenters.AuthResponse;
import br.com.oficina.auth.presenters.UsuarioResponse;
import br.com.oficina.auth.usecases.LoginResult;
import br.com.oficina.auth.usecases.LoginUseCase;
import br.com.oficina.auth.usecases.RegisterCommand;
import br.com.oficina.auth.usecases.RegistrarUsuarioUseCase;
import br.com.oficina.shared.domain.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Registro e login de usuários")
@SecurityRequirements
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegistrarUsuarioUseCase registrarUsuario;
    private final AuthPresenter presenter;
    private final long expirationMs;

    public AuthController(LoginUseCase loginUseCase,
                          RegistrarUsuarioUseCase registrarUsuario,
                          AuthPresenter presenter,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.loginUseCase = loginUseCase;
        this.registrarUsuario = registrarUsuario;
        this.presenter = presenter;
        this.expirationMs = expirationMs;
    }

    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário (ADMIN ou MECANICO)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Username já em uso",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Public registration always creates MECANICO accounts; ADMIN must be created
        // via AdminInitializer or directly by another admin through the database.
        RegisterCommand command = new RegisterCommand(request.username(), request.password(), Role.MECANICO);
        Usuario usuario = registrarUsuario.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.present(usuario));
    }

    @Operation(summary = "Login", description = "Autentica o usuário e retorna um token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.execute(request.username(), request.password());
        return ResponseEntity.ok(presenter.present(result, expirationMs));
    }
}
