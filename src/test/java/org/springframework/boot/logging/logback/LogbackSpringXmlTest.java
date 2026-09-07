package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.Status;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.StandardEnvironment;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que o {@code logback-spring.xml} de {@code oficina-api} é válido
 * de verdade, carregando-o com o mesmo configurador que o Spring Boot usa em
 * produção ({@link SpringBootJoranConfigurator}) — é ele quem sabe interpretar
 * a tag {@code <springProfile>}, resolvendo-a contra um
 * {@link org.springframework.core.env.Environment} real.
 *
 * <p>Esta classe fica de propósito no pacote
 * {@code org.springframework.boot.logging.logback} (mesmo pacote de
 * {@link SpringBootJoranConfigurator}), pois essa classe é package-private —
 * não faz parte da API pública do Spring Boot. É a forma legítima de reusar,
 * em teste, o mesmo mecanismo de parsing que a aplicação real usa ao subir.
 *
 * <p>Isso substitui a verificação manual proposta originalmente no brief
 * (subir a aplicação com {@code spring-boot:run} e fazer um curl): o ambiente
 * de execução deste agente não tem banco de dados disponível (Docker Desktop
 * não está rodando), então a aplicação não sobe. Um typo no nome da classe do
 * appender/encoder (ex.: {@code LogstashEncoderXyz}) ou um atributo inválido
 * gera um {@link Status#ERROR} no {@code StatusManager} do Logback durante o
 * parse — é exatamente essa falha que este teste pega, sem precisar de
 * infraestrutura nenhuma.
 *
 * <p>Cada teste ativa um perfil diferente antes de carregar o arquivo, de
 * modo que o branch {@code <springProfile>} correspondente seja realmente
 * avaliado (não apenas "bem formado como XML") — cobrindo tanto o branch
 * dev/test (console legível) quanto o branch homolog/prod (JSON).
 */
class LogbackSpringXmlTest {

    private static final File LOGBACK_SPRING_XML =
            new File("src/main/resources/logback-spring.xml");

    private LoggerContext context;

    @AfterEach
    void encerrarContexto() {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void configuracaoNaoDeveGerarErroComPerfilDev() throws Exception {
        assertConfiguracaoSemErro("dev");
    }

    @Test
    void configuracaoNaoDeveGerarErroComPerfilTest() throws Exception {
        assertConfiguracaoSemErro("test");
    }

    @Test
    void configuracaoNaoDeveGerarErroComPerfilHomolog() throws Exception {
        assertConfiguracaoSemErro("homolog");
    }

    @Test
    void configuracaoNaoDeveGerarErroComPerfilProd() throws Exception {
        assertConfiguracaoSemErro("prod");
    }

    @Test
    void perfilDesconhecidoDeveTerAppenderDeFallback() throws Exception {
        // Regressão: um perfil que não seja dev/test/homolog/prod (ex.: um
        // typo de overlay futuro) não pode ficar sem NENHUM appender — isso
        // significaria perder toda linha de log em silêncio, sem erro algum
        // no start-up. Ver bloco de fallback em logback-spring.xml.
        carregarConfiguracao("staging-com-typo");

        assertThat(obterAppenderDoRoot())
                .as("logger root deveria ter um appender de fallback mesmo com um perfil desconhecido")
                .isNotNull();
    }

    @Test
    void perfilProdDeveUsarLogstashEncoderComMdcDeCorrelacao() throws Exception {
        carregarConfiguracao("prod");

        var appender = obterAppenderDoRoot();
        assertThat(appender).isInstanceOf(ConsoleAppender.class);

        var encoder = ((ConsoleAppender<?>) appender).getEncoder();
        assertThat(encoder).isInstanceOf(LogstashEncoder.class);

        var logstashEncoder = (LogstashEncoder) encoder;
        // As duas chaves que o CorrelationIdFilter põe no MDC precisam
        // aparecer no JSON — senão a correlação declarada no enunciado da
        // Fase 3 não existe de fato na saída de log.
        assertThat(logstashEncoder.getIncludeMdcKeyNames())
                .contains("correlationId", "clienteId");
        assertThat(logstashEncoder.getCustomFields()).contains("oficina-api");
    }

    @Test
    void perfilDevDeveUsarConsoleComCorrelationIdNoPadrao() throws Exception {
        carregarConfiguracao("dev");

        var appender = obterAppenderDoRoot();
        assertThat(appender).isInstanceOf(ConsoleAppender.class);

        var encoder = ((ConsoleAppender<?>) appender).getEncoder();
        assertThat(encoder).isInstanceOf(PatternLayoutEncoder.class);
        assertThat(((PatternLayoutEncoder) encoder).getPattern()).contains("%X{correlationId}");
    }

    private void assertConfiguracaoSemErro(String perfilAtivo) throws Exception {
        carregarConfiguracao(perfilAtivo);

        List<Status> statusComErro = context.getStatusManager().getCopyOfStatusList().stream()
                .filter(status -> status.getLevel() == Status.ERROR)
                .toList();

        assertThat(statusComErro)
                .as("logback-spring.xml não deve gerar status ERROR ao ser carregado com o perfil '%s'; encontrado: %s",
                        perfilAtivo, statusComErro)
                .isEmpty();
    }

    private void carregarConfiguracao(String perfilAtivo) throws Exception {
        var environment = new StandardEnvironment();
        environment.setActiveProfiles(perfilAtivo);

        context = new LoggerContext();
        context.setName("teste-" + perfilAtivo);

        var configurator = new SpringBootJoranConfigurator(new LoggingInitializationContext(environment));
        configurator.setContext(context);
        configurator.doConfigure(LOGBACK_SPRING_XML);
    }

    private Appender<?> obterAppenderDoRoot() {
        var root = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        var appenders = root.iteratorForAppenders();
        assertThat(appenders.hasNext())
                .as("logger root deveria ter ao menos um appender configurado")
                .isTrue();
        return appenders.next();
    }
}
