package genius.project.orchestrate;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(OrchestrateApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void choreWebAdapterUsesOnlyPublicServiceOfItsModule() {
        assertThat(modules.getModuleByName("chore")).isPresent();

        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("genius.project.orchestrate");

        noClasses().that().resideInAPackage("..chore.web..")
                .should().dependOnClassesThat().resideInAPackage("..chore.internal..")
                .because("веб-адаптер має звертатися лише до публічного ChoreLifecycleService")
                .check(production);

        noClasses().that().resideOutsideOfPackage("..chore.web..")
                .should().dependOnClassesThat().resideInAPackage("..chore.web..")
                .because("класи chore.web — деталь реалізації веб-шару, а не API модуля")
                .check(production);
    }

    @Test
    void writesDocumentationSnapshot() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
