package br.com.oficina.auth.infrastructure;

import br.com.oficina.auth.gateways.TokenGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.RequiredTypeException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtService implements TokenGateway {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrai o {@code role} do token.
     *
     * <p>Uma claim {@code role} presente mas com tipo incompatível (ex.: número, objeto)
     * é tratada como role inutilizável: retorna {@code null}, igual ao caso de claim
     * ausente, em vez de propagar {@link RequiredTypeException}. Um token com role
     * ilegível é um token sem role — cabe ao chamador ({@link JwtAuthenticationFilter})
     * não autenticar a requisição nesse caso, e não construir uma authority a partir de
     * um valor nulo/inválido. O mesmo vale para uma claim presente mas em branco
     * ({@code ""}): sem normalização, ela produziria a authority real "ROLE_", que não
     * corresponde a nada e leria como uma requisição autenticada.
     */
    public String extractRole(String token) {
        try {
            String role = parseClaims(token).get("role", String.class);
            return (role == null || role.isBlank()) ? null : role;
        } catch (RequiredTypeException e) {
            return null;
        }
    }

    /**
     * Extrai o {@code clienteId} do token, quando presente e utilizável.
     *
     * <p>Tokens emitidos pela Lambda de autenticação de clientes trazem essa claim.
     * Tokens emitidos por {@code /auth/login} para operadores (admin/atendente) não a
     * possuem — nesse caso o retorno é {@link Optional#empty()}, sem invalidar o token.
     *
     * <p>Uma claim {@code clienteId} presente mas com tipo incompatível (ex.: número,
     * objeto) também é tratada como identidade de cliente inutilizável — mesmo contrato
     * de claim ausente, {@link Optional#empty()} — em vez de propagar
     * {@link RequiredTypeException} e derrubar a requisição com 500. O mesmo vale para
     * uma claim presente mas em branco ({@code ""}): nada a jusante valida "não branco",
     * então uma string vazia não pode ser tratada como identidade de cliente válida.
     */
    public Optional<String> extractClienteId(String token) {
        try {
            return Optional.ofNullable(parseClaims(token).get("clienteId", String.class))
                    .filter(clienteId -> !clienteId.isBlank());
        } catch (RequiredTypeException e) {
            return Optional.empty();
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
