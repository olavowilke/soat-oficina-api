package br.com.oficina.peca.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PecaJpaRepository extends JpaRepository<PecaData, UUID> {
    List<PecaData> findByAtivoTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PecaData p set p.quantidadeEstoque = p.quantidadeEstoque + :n, p.atualizadoEm = CURRENT_TIMESTAMP where p.id = :id")
    int somarEstoque(@Param("id") UUID id, @Param("n") int n);
}
