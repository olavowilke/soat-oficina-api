package br.com.oficina.auth.usecases;

import br.com.oficina.auth.entities.Usuario;
import br.com.oficina.auth.gateways.AutenticadorGateway;
import br.com.oficina.auth.gateways.TokenGateway;
import br.com.oficina.auth.gateways.UsuarioGateway;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private final UsuarioGateway usuarioGateway;
    private final TokenGateway tokenGateway;
    private final AutenticadorGateway autenticadorGateway;

    public LoginUseCase(UsuarioGateway usuarioGateway, TokenGateway tokenGateway,
                        AutenticadorGateway autenticadorGateway) {
        this.usuarioGateway = usuarioGateway;
        this.tokenGateway = tokenGateway;
        this.autenticadorGateway = autenticadorGateway;
    }

    public LoginResult execute(String username, String password) {
        autenticadorGateway.autenticar(username, password);
        Usuario usuario = usuarioGateway.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        String token = tokenGateway.generateToken(usuario.getUsername(), usuario.getRole().name());
        return new LoginResult(token, usuario.getUsername(), usuario.getRole());
    }
}
