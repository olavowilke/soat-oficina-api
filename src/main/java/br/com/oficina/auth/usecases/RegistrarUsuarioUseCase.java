package br.com.oficina.auth.usecases;

import br.com.oficina.auth.entities.Usuario;
import br.com.oficina.auth.gateways.UsuarioGateway;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrarUsuarioUseCase {

    private final UsuarioGateway usuarioGateway;
    private final PasswordEncoder passwordEncoder;

    public RegistrarUsuarioUseCase(UsuarioGateway usuarioGateway, PasswordEncoder passwordEncoder) {
        this.usuarioGateway = usuarioGateway;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario execute(RegisterCommand command) {
        if (usuarioGateway.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username já está em uso: " + command.username());
        }
        String hash = passwordEncoder.encode(command.password());
        Usuario usuario = Usuario.novo(command.username(), hash, command.role());
        return usuarioGateway.save(usuario);
    }
}
