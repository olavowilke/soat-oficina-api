package br.com.oficina.cliente.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.gateways.ClienteGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarClientesUseCase {

    private final ClienteGateway clienteGateway;

    public ListarClientesUseCase(ClienteGateway clienteGateway) {
        this.clienteGateway = clienteGateway;
    }

    @Transactional(readOnly = true)
    public List<Cliente> execute() {
        return clienteGateway.findAllAtivos();
    }
}
