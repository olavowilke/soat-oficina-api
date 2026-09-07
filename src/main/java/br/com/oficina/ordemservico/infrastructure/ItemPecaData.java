package br.com.oficina.ordemservico.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itens_peca")
@Getter
@Setter
@NoArgsConstructor
public class ItemPecaData {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServicoData ordemServico;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Column(name = "nome_peca", nullable = false)
    private String nomePeca;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;
}
