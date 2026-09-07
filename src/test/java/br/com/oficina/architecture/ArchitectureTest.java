package br.com.oficina.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Testes da regra de dependência da Clean Architecture (ver ADR 0001).
 *
 * <p>As regras miram os subpacotes das camadas ({@code ..entities..}, {@code ..usecases..},
 * {@code ..gateways..}) e cobrem automaticamente cada bounded context à medida que é migrado.
 * Com a Etapa 2 concluída, cobrem os <b>6</b> contextos: {@code servico}, {@code ordemservico},
 * {@code cliente}, {@code veiculo}, {@code peca} e {@code auth}. Neste último, o acoplamento ao
 * Spring Security foi isolado por ports ({@code TokenGateway}, {@code AutenticadorGateway}) para
 * manter os Use Cases livres de {@code infrastructure}.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.oficina");
    }

    @Test
    void entitiesDevemSerLivresDeFramework() {
        noClasses().that().resideInAPackage("..entities..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..")
                .as("Entities (núcleo) não devem depender de Spring nem de JPA")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void entitiesNaoDependemDeCamadasExternas() {
        noClasses().that().resideInAPackage("..entities..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..usecases..", "..gateways..", "..controllers..",
                        "..presenters..", "..infrastructure..")
                .as("Entities não devem depender de camadas externas (dependência aponta para dentro)")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void usecasesNaoDependemDeFrameworksExternosNemDeAdapters() {
        noClasses().that().resideInAPackage("..usecases..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..", "org.springframework.web..",
                        "..infrastructure..", "..controllers..", "..presenters..")
                .as("Use Cases não devem depender de JPA, da web nem dos adapters de entrada/saída")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void gatewaysSaoPortsLimpos() {
        noClasses().that().resideInAPackage("..gateways..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..", "org.springframework..",
                        "..infrastructure..", "..controllers..", "..presenters..")
                .as("Gateways (ports) não devem depender de framework nem de adapters")
                .allowEmptyShould(true)
                .check(classes);
    }
}
