package br.com.oficina.auth.gateways;

/**
 * Port (Gateway) de autenticação de credenciais. Abstrai o mecanismo concreto
 * (Spring Security {@code AuthenticationManager}), mantendo o Use Case desacoplado
 * da infraestrutura. Lança exceção quando as credenciais são inválidas.
 */
public interface AutenticadorGateway {

    void autenticar(String username, String password);
}
