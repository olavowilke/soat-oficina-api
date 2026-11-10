package br.com.oficina.shared.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga o agendador usado pelo OutboxRelay. */
@Configuration
@EnableScheduling
public class MensageriaConfig {
}
