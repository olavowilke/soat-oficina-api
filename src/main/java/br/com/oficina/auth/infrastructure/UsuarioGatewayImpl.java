package br.com.oficina.auth.infrastructure;

import br.com.oficina.auth.entities.Usuario;
import br.com.oficina.auth.gateways.UsuarioGateway;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioGatewayImpl implements UsuarioGateway {

    private final UsuarioJpaRepository jpaRepository;

    public UsuarioGatewayImpl(UsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Usuario save(Usuario usuario) {
        return UsuarioMapper.toDomain(jpaRepository.save(UsuarioMapper.toData(usuario)));
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(UsuarioMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }
}
