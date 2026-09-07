package br.com.oficina.auth.gateways;

import br.com.oficina.auth.entities.Usuario;

import java.util.Optional;

/**
 * Port (Gateway) do contexto de autenticação. Isola o núcleo do mecanismo de persistência.
 * A implementação (Infra) traduz entre {@link Usuario} (domínio) e o modelo JPA.
 */
public interface UsuarioGateway {

    Usuario save(Usuario usuario);

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
