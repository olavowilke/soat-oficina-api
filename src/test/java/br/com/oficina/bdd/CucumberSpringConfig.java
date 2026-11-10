package br.com.oficina.bdd;

import br.com.oficina.shared.BaseControllerIT;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Configuração de contexto do Cucumber. Estende {@link BaseControllerIT} de propósito: herda as
 * mesmas anotações e o mesmo {@code @DynamicPropertySource} (mesmos containers). Assim o cache do
 * Spring devolve o mesmo contexto dos outros ITs e não sobe um segundo RespostasListener na fila.
 * Não adicione @MockitoBean, @TestPropertySource nem outra anotação que mude o contexto.
 */
@CucumberContextConfiguration
public class CucumberSpringConfig extends BaseControllerIT {
}
