package br.com.oficina.veiculo.infrastructure;

import br.com.oficina.veiculo.entities.Veiculo;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VeiculoGatewayImpl implements VeiculoGateway {

    private final VeiculoJpaRepository jpaRepository;

    public VeiculoGatewayImpl(VeiculoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Veiculo> findById(UUID id) {
        return jpaRepository.findById(id).map(VeiculoMapper::toDomain);
    }

    @Override
    public Optional<Veiculo> findByPlaca(String placa) {
        return jpaRepository.findByPlaca(placa).map(VeiculoMapper::toDomain);
    }

    @Override
    public List<Veiculo> findByClienteId(UUID clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream()
                .map(VeiculoMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPlaca(String placa) {
        return jpaRepository.existsByPlaca(placa);
    }

    @Override
    public Veiculo save(Veiculo veiculo) {
        VeiculoData data = VeiculoMapper.toData(veiculo);
        VeiculoData saved = jpaRepository.save(data);
        return VeiculoMapper.toDomain(saved);
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
