package br.com.oficinamecanica.shared;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Regra de dependencia da Clean Architecture")
class ArquiteturaTest {

    private static final String PACOTE_RAIZ = "br.com.oficinamecanica";
    private static final String ENTIDADES = "br.com.oficinamecanica..domain..";
    private static final String CASOS_DE_USO = "br.com.oficinamecanica..application..";
    private static final String BORDA_REST = "br.com.oficinamecanica..api..";
    private static final String INFRAESTRUTURA = "br.com.oficinamecanica..infrastructure..";
    private static final String[] FRAMEWORKS = {"org.springframework..", "jakarta..", "io.swagger..", "tools.jackson.."};
    private static final String MOTIVO = "as dependencias de codigo devem sempre apontar para o centro";

    private static JavaClasses classesDeProducao;

    @BeforeAll
    static void importarClassesDeProducao() {
        classesDeProducao = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(PACOTE_RAIZ);
    }

    @Test
    @DisplayName("deve manter as entidades sem framework, sem borda e sem infraestrutura")
    void deveManterAsEntidadesNoCentro() {
        noClasses().that().resideInAPackage(ENTIDADES)
                .should().dependOnClassesThat().resideInAnyPackage(aneisExternos())
                .because(MOTIVO)
                .check(classesDeProducao);
    }

    @Test
    @DisplayName("deve manter os casos de uso sem framework, sem borda e sem infraestrutura")
    void deveManterOsCasosDeUsoSemFramework() {
        noClasses().that().resideInAPackage(CASOS_DE_USO)
                .should().dependOnClassesThat().resideInAnyPackage(aneisExternos())
                .because(MOTIVO)
                .check(classesDeProducao);
    }

    @Test
    @DisplayName("deve manter a borda REST independente da infraestrutura")
    void deveManterABordaRestIndependenteDaInfraestrutura() {
        noClasses().that().resideInAPackage(BORDA_REST)
                .should().dependOnClassesThat().resideInAPackage(INFRAESTRUTURA)
                .because(MOTIVO)
                .check(classesDeProducao);
    }

    private static String[] aneisExternos() {
        return Stream.concat(Stream.of(FRAMEWORKS), Stream.of(BORDA_REST, INFRAESTRUTURA)).toArray(String[]::new);
    }
}
