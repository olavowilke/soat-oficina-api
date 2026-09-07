package br.com.oficina.auth.infrastructure;

import br.com.oficina.auth.gateways.AutenticadorGateway;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Adapter (Infra) do {@link AutenticadorGateway} sobre o Spring Security
 * {@code AuthenticationManager}. Propaga {@code BadCredentialsException} quando as
 * credenciais são inválidas (mapeada para 401 no {@code GlobalExceptionHandler}).
 */
@Component
public class AutenticadorGatewayImpl implements AutenticadorGateway {

    private final AuthenticationManager authenticationManager;

    public AutenticadorGatewayImpl(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void autenticar(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
    }
}
