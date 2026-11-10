package br.com.oficina.peca.infrastructure;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void devolverAoEstoque(UUID id, int quantidade) {
        if (jpaRepository.somarEstoque(id, quantidade) == 0) {
            throw new RecursoNaoEncontradoException("Peça", id);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
