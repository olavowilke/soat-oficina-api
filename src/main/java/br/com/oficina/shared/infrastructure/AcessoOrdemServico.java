package br.com.oficina.shared.infrastructure;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Decide se quem fez a requisição pode tocar numa Ordem de Serviço específica.
 *
 * <p>Os matchers de {@link SecurityConfig} resolvem o papel: operação de
 * oficina é de operador. Sobram as rotas que um cliente usa na própria ordem —
 * consultar, aprovar e recusar orçamento. Ali o papel não basta: todo cliente
 * tem o mesmo papel {@code CLIENTE}, então sem esta checagem um cliente lê e
 * decide o orçamento de outro.
 *
 * <p>A identidade vem da claim {@code clienteId} do JWT, que
 * {@link br.com.oficina.auth.infrastructure.JwtAuthenticationFilter} guarda nos
 * details da autenticação. É uma claim assinada, não um cabeçalho HTTP: o
 * cabeçalho {@code X-Cliente-Id} que o API Gateway envia serve só para log
 * (ver {@link CorrelationIdFilter}) e qualquer chamada dentro da VPC poderia
 * forjá-lo.
 *
 * <p>Nega por omissão. Sem autenticação, sem papel conhecido, sem
 * {@code clienteId} no token ou sem dono na ordem, o acesso não passa.
 */
@Component
public class AcessoOrdemServico {

    /** Papéis que operam a oficina e enxergam qualquer ordem. */
    private static final Set<String> PAPEIS_DE_OPERADOR =
            Set.of("ROLE_ADMIN", "ROLE_MECANICO");

    /**
     * @param clienteIdDaOrdem dono da ordem que a requisição quer acessar.
     * @throws AccessDeniedException se quem chamou não for operador nem o dono.
     */
    public void exigirAcesso(UUID clienteIdDaOrdem) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Acesso negado");
        }

        boolean operador = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PAPEIS_DE_OPERADOR::contains);
        if (operador) {
            return;
        }

        if (clienteIdDaOrdem == null) {
            throw new AccessDeniedException("Acesso negado");
        }

        Object details = auth.getDetails();
        if (!(details instanceof String clienteIdDoToken) || clienteIdDoToken.isBlank()) {
            throw new AccessDeniedException("Acesso negado");
        }

        if (!clienteIdDaOrdem.toString().equals(clienteIdDoToken)) {
            throw new AccessDeniedException("Acesso negado");
        }
    }
}
