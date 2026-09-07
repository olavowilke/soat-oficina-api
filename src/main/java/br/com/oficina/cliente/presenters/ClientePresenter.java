package br.com.oficina.cliente.presenters;

import br.com.oficina.cliente.entities.Cliente;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Presenter do contexto de clientes: converte a saída dos Use Cases ({@link Cliente})
 * no ViewModel {@link ClienteResponse}. Não conhece HTTP/{@code ResponseEntity}.
 */
@Component
public class ClientePresenter {

    public ClienteResponse present(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getDocumento().numero(),
                cliente.getDocumento().tipo(),
                cliente.isAtivo(),
                cliente.getCriadoEm()
        );
    }

    public List<ClienteResponse> present(List<Cliente> clientes) {
        return clientes.stream().map(this::present).toList();
    }
}
