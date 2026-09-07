package br.com.oficina.shared.domain;

/**
 * Lançada quando uma requisição de webhook não apresenta um token válido,
 * indicando origem não confiável. Mapeada para HTTP 401.
 */
public class AutenticacaoWebhookException extends RuntimeException {

    public AutenticacaoWebhookException(String message) {
        super(message);
    }
}
