package br.com.oficina.cliente.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.entities.Documento;
import br.com.oficina.cliente.gateways.ClienteGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarClienteUseCase {

    private final ClienteGateway clienteGateway;

    public CadastrarClienteUseCase(ClienteGateway clienteGateway) {
        this.clienteGateway = clienteGateway;
    }

    @Transactional
    public Cliente execute(CadastrarClienteCommand command) {
        String numeroLimpo = command.documento().replaceAll("[^0-9]", "");
        if (clienteGateway.existsByDocumento(numeroLimpo)) {
            throw new IllegalArgumentException("Já existe um cliente com o documento informado");
        }
        Documento documento = new Documento(command.documento(), command.tipoDocumento());
        Cliente cliente = Cliente.novo(command.nome(), command.email(), command.telefone(), documento);
        return clienteGateway.save(cliente);
    }
}
