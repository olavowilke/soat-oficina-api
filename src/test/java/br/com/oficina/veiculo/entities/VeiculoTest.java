package br.com.oficina.veiculo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class VeiculoTest {

    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final Placa PLACA = new Placa("ABC1234");

    @Test
    void deveCriarVeiculoComDadosValidos() {
        Veiculo v = Veiculo.novo(CLIENTE_ID, PLACA, "Toyota", "Corolla", 2020, "Prata");

        assertThat(v.getId()).isNotNull();
        assertThat(v.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(v.getPlaca()).isEqualTo(PLACA);
        assertThat(v.getMarca()).isEqualTo("Toyota");
        assertThat(v.getModelo()).isEqualTo("Corolla");
        assertThat(v.getAno()).isEqualTo(2020);
        assertThat(v.getCor()).isEqualTo("Prata");
        assertThat(v.getCriadoEm()).isNotNull();
        assertThat(v.getAtualizadoEm()).isNotNull();
    }

    @Test
    void deveLancarExcecaoSemClienteId() {
        assertThatThrownBy(() -> Veiculo.novo(null, PLACA, "Toyota", "Corolla", 2020, "Prata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ClienteId");
    }

    @Test
    void deveLancarExcecaoSemPlaca() {
        assertThatThrownBy(() -> Veiculo.novo(CLIENTE_ID, null, "Toyota", "Corolla", 2020, "Prata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Placa");
    }

    @Test
    void deveLancarExcecaoSemMarca() {
        assertThatThrownBy(() -> Veiculo.novo(CLIENTE_ID, PLACA, "", "Corolla", 2020, "Prata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Marca");
    }

    @Test
    void deveLancarExcecaoComMarcaNula() {
        assertThatThrownBy(() -> Veiculo.novo(CLIENTE_ID, PLACA, null, "Corolla", 2020, "Prata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Marca");
    }

    @Test
    void deveLancarExcecaoSemModelo() {
        assertThatThrownBy(() -> Veiculo.novo(CLIENTE_ID, PLACA, "Toyota", "", 2020, "Prata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modelo");
    }

    @Test
    void deveLancarExcecaoComModeloNulo() {
        assertThatThrownBy(() -> Veiculo.novo(CLIENTE_ID, PLACA, "Toyota", null, 2020, "Prata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modelo");
    }

    @Test
    void deveAtualizarDadosDoVeiculo() {
        Veiculo v = Veiculo.novo(CLIENTE_ID, PLACA, "Toyota", "Corolla", 2020, "Prata");
        LocalDateTime before = v.getAtualizadoEm();

        v.atualizar("Honda", "Civic", 2022, "Preto");

        assertThat(v.getMarca()).isEqualTo("Honda");
        assertThat(v.getModelo()).isEqualTo("Civic");
        assertThat(v.getAno()).isEqualTo(2022);
        assertThat(v.getCor()).isEqualTo("Preto");
        assertThat(v.getAtualizadoEm()).isAfterOrEqualTo(before);
    }

    @Test
    void deveLancarExcecaoAoAtualizarComMarcaInvalida() {
        Veiculo v = Veiculo.novo(CLIENTE_ID, PLACA, "Toyota", "Corolla", 2020, "Prata");

        assertThatThrownBy(() -> v.atualizar("", "Civic", 2022, "Preto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Marca");
    }

    @Test
    void deveLancarExcecaoAoAtualizarComModeloInvalido() {
        Veiculo v = Veiculo.novo(CLIENTE_ID, PLACA, "Toyota", "Corolla", 2020, "Prata");

        assertThatThrownBy(() -> v.atualizar("Honda", "", 2022, "Preto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modelo");
    }

    @Test
    void deveReconstituirVeiculoComTodosOsCampos() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Veiculo v = Veiculo.reconstituir(id, CLIENTE_ID, PLACA, "Toyota", "Corolla", 2020, "Prata", now, now);

        assertThat(v.getId()).isEqualTo(id);
        assertThat(v.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(v.getPlaca()).isEqualTo(PLACA);
        assertThat(v.getMarca()).isEqualTo("Toyota");
        assertThat(v.getModelo()).isEqualTo("Corolla");
        assertThat(v.getAno()).isEqualTo(2020);
        assertThat(v.getCor()).isEqualTo("Prata");
        assertThat(v.getCriadoEm()).isEqualTo(now);
        assertThat(v.getAtualizadoEm()).isEqualTo(now);
    }
}
