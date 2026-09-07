package br.com.oficina.servico.infrastructure;

import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ServicoGatewayImpl implements ServicoGateway {

    private final ServicoJpaRepository jpaRepository;

    public ServicoGatewayImpl(ServicoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Servico> findById(UUID id) {
        return jpaRepository.findById(id).map(ServicoMapper::toDomain);
    }

    @Override
    public List<Servico> findAll() {
        return jpaRepository.findAll().stream()
                .map(ServicoMapper::toDomain)
                .toList();
    }

    @Override
    public List<Servico> findAllAtivos() {
        return jpaRepository.findByAtivoTrue().stream()
                .map(ServicoMapper::toDomain)
                .toList();
    }

    @Override
    public Servico save(Servico servico) {
        ServicoData data = ServicoMapper.toData(servico);
        ServicoData saved = jpaRepository.save(data);
        return ServicoMapper.toDomain(saved);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
