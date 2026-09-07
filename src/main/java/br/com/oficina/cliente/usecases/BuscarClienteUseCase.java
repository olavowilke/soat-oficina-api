package br.com.oficina.cliente.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarClienteUseCase {

    private final ClienteGateway clienteGateway;

    public BuscarClienteUseCase(ClienteGateway clienteGateway) {
        this.clienteGateway = clienteGateway;
    }

    @Transactional(readOnly = true)
    public Cliente execute(UUID id) {
        return clienteGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }
}
