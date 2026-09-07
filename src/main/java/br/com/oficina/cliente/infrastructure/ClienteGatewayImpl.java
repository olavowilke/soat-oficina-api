package br.com.oficina.cliente.infrastructure;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.gateways.ClienteGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ClienteGatewayImpl implements ClienteGateway {

    private final ClienteJpaRepository jpaRepository;

    public ClienteGatewayImpl(ClienteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Cliente> findById(UUID id) {
        return jpaRepository.findById(id).map(ClienteMapper::toDomain);
    }

    @Override
    public Optional<Cliente> findByDocumento(String numero) {
        return jpaRepository.findByDocumento(numero).map(ClienteMapper::toDomain);
    }

    @Override
    public boolean existsByDocumento(String numero) {
        return jpaRepository.existsByDocumento(numero);
    }

    @Override
    public List<Cliente> findAll() {
        return jpaRepository.findAll().stream()
                .map(ClienteMapper::toDomain)
                .toList();
    }

    @Override
    public List<Cliente> findAllAtivos() {
        return jpaRepository.findByAtivoTrue().stream()
                .map(ClienteMapper::toDomain)
                .toList();
    }

    @Override
    public Cliente save(Cliente cliente) {
        ClienteData data = ClienteMapper.toData(cliente);
        ClienteData saved = jpaRepository.save(data);
        return ClienteMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
