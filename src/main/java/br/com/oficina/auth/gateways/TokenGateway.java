package br.com.oficina.auth.gateways;

/**
 * Port (Gateway) de emissão de tokens de acesso. Mantém os Use Cases livres do
 * mecanismo concreto de token (JWT), cuja implementação vive na camada de infraestrutura.
 */
public interface TokenGateway {

    String generateToken(String username, String role);
}
