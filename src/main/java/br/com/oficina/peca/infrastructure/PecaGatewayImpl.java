package br.com.oficina.peca.infrastructure;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PecaGatewayImpl implements PecaGateway {

    private final PecaJpaRepository jpaRepository;

    public PecaGatewayImpl(PecaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Peca> findById(UUID id) {
        return jpaRepository.findById(id).map(PecaMapper::toDomain);
    }

    @Override
    public List<Peca> findAll() {
        return jpaRepository.findAll().stream()
                .map(PecaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Peca> findAllAtivos() {
        return jpaRepository.findByAtivoTrue().stream()
                .map(PecaMapper::toDomain)
                .toList();
    }

    @Override
    public Peca save(Peca peca) {
        PecaData data = PecaMapper.toData(peca);
        PecaData saved = jpaRepository.save(data);
        return PecaMapper.toDomain(saved);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
