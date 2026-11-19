package app.lyra.architecture;

import archfixture.food.adapter.out.persistence.FoodEntity;
import archfixture.food.application.service.FoodSearchService;
import archfixture.food.domainevents.FoodRenamed;
import archfixture.violation.adapter.in.web.ControllerUsingOutboundAdapter;
import archfixture.violation.application.service.ServiceUsingAdapter;
import archfixture.violation.application.service.ServiceUsingLookalikePackage;
import archfixture.violation.application.service.ServiceUsingOtherModuleService;
import archfixture.violation.domain.DomainUsingAdapter;
import archfixture.violation.domain.DomainUsingFramework;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HexagonalArchitectureTest {

    private static final String PRODUCTION_ROOT = "app.lyra";
    private static final String FIXTURE_ROOT = "archfixture";

    private static final List<Function<String, ArchRule>> RULES = List.of(
            HexagonalRules::domainDependsOnlyOnJdkAndDomain,
            HexagonalRules::applicationDoesNotDependOnAdapters,
            HexagonalRules::inboundAdaptersDoNotDependOnOutboundAdapters,
            HexagonalRules::modulesTalkOnlyThroughInboundPortsAndDomain);

    @Test
    void productionCodeFollowsAllRules() {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(PRODUCTION_ROOT);

        RULES.forEach(rule -> rule.apply(PRODUCTION_ROOT).check(production));
    }

    @Test
    void validModulesPassEveryRule() {
        JavaClasses valid = new ClassFileImporter()
                .importPackages(FIXTURE_ROOT + ".food", FIXTURE_ROOT + ".diary");

        RULES.forEach(rule -> assertThatCode(() -> rule.apply(FIXTURE_ROOT).check(valid))
                .doesNotThrowAnyException());
    }

    @Nested
    class RulesRejectViolations {

        @Test
        void domainDependingOnAnAdapterIsRejected() {
            assertViolation(HexagonalRules.domainDependsOnlyOnJdkAndDomain(FIXTURE_ROOT),
                    DomainUsingAdapter.class, FoodEntity.class);
        }

        @Test
        void domainDependingOnAFrameworkIsRejected() {
            assertViolation(HexagonalRules.domainDependsOnlyOnJdkAndDomain(FIXTURE_ROOT),
                    DomainUsingFramework.class, Component.class);
        }

        @Test
        void applicationDependingOnAnAdapterIsRejected() {
            assertViolation(HexagonalRules.applicationDoesNotDependOnAdapters(FIXTURE_ROOT),
                    ServiceUsingAdapter.class, FoodEntity.class);
        }

        @Test
        void inboundAdapterDependingOnAnOutboundAdapterIsRejected() {
            assertViolation(HexagonalRules.inboundAdaptersDoNotDependOnOutboundAdapters(FIXTURE_ROOT),
                    ControllerUsingOutboundAdapter.class, FoodEntity.class);
        }

        @Test
        void moduleReachingAPackageThatOnlyStartsLikeDomainIsRejected() {
            assertViolation(HexagonalRules.modulesTalkOnlyThroughInboundPortsAndDomain(FIXTURE_ROOT),
                    ServiceUsingLookalikePackage.class, FoodRenamed.class);
        }

        @Test
        void moduleReachingAnotherModuleServiceIsRejected() {
            assertViolation(HexagonalRules.modulesTalkOnlyThroughInboundPortsAndDomain(FIXTURE_ROOT),
                    ServiceUsingOtherModuleService.class, FoodSearchService.class);
        }

        private void assertViolation(ArchRule rule, Class<?> violatingClass, Class<?> target) {
            JavaClasses classes = new ClassFileImporter().importClasses(violatingClass, target);

            assertThatThrownBy(() -> rule.check(classes))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(violatingClass.getSimpleName())
                    .hasMessageContaining(target.getSimpleName());
        }
    }
}
