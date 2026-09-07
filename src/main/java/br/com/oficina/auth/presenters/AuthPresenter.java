package br.com.oficina.auth.presenters;

import br.com.oficina.auth.entities.Usuario;
import br.com.oficina.auth.usecases.LoginResult;
import org.springframework.stereotype.Component;

/**
 * Presenter do contexto de autenticação: converte a saída dos Use Cases
 * ({@link LoginResult}, {@link Usuario}) nos ViewModels {@link AuthResponse} /
 * {@link UsuarioResponse}. Não conhece HTTP/{@code ResponseEntity}.
 */
@Component
public class AuthPresenter {

    public AuthResponse present(LoginResult result, long expiresInMs) {
        return new AuthResponse(result.token(), result.username(), result.role().name(), expiresInMs);
    }

    public UsuarioResponse present(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(),
                usuario.getRole(), usuario.isAtivo(), usuario.getCriadoEm());
    }
}
