package br.com.oficina.ordemservico.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itens_servico")
@Getter
@Setter
@NoArgsConstructor
public class ItemServicoData {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServicoData ordemServico;

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Column(name = "nome_servico", nullable = false)
    private String nomeServico;

    @Column(name = "valor_cobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorCobrado;
}
