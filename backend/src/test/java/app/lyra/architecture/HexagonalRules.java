package app.lyra.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Optional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

final class HexagonalRules {

    private HexagonalRules() {
    }

    static ArchRule domainDependsOnlyOnJdkAndDomain(String root) {
        return classes().that().resideInAPackage(root + ".*.domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "lombok..", root + ".*.domain..")
                .allowEmptyShould(true);
    }

    static ArchRule applicationDoesNotDependOnAdapters(String root) {
        return noClasses().that().resideInAPackage(root + ".*.application..")
                .should().dependOnClassesThat().resideInAPackage(root + ".*.adapter..")
                .allowEmptyShould(true);
    }

    static ArchRule inboundAdaptersDoNotDependOnOutboundAdapters(String root) {
        return noClasses().that().resideInAPackage(root + ".*.adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(root + ".*.adapter.out..")
                .allowEmptyShould(true);
    }

    static ArchRule modulesTalkOnlyThroughInboundPortsAndDomain(String root) {
        return classes().that().resideInAPackage(root + ".*..")
                .should(onlyReachOtherModulesThroughInboundPortsOrDomain(root))
                .allowEmptyShould(true);
    }

    private static ArchCondition<JavaClass> onlyReachOtherModulesThroughInboundPortsOrDomain(String root) {
        return new ArchCondition<>("only depend on other modules through application.port.in or domain") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> originModule = moduleOf(root, origin.getPackageName());
                if (originModule.isEmpty()) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    Optional<String> targetModule = moduleOf(root, targetPackage);
                    if (targetModule.isEmpty() || targetModule.equals(originModule)) {
                        continue;
                    }
                    String inModule = targetPackage.substring((root + "." + targetModule.get()).length());
                    boolean allowed = inModule.startsWith(".application.port.in") || inModule.startsWith(".domain");
                    if (!allowed) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static Optional<String> moduleOf(String root, String packageName) {
        String prefix = root + ".";
        if (!packageName.startsWith(prefix)) {
            return Optional.empty();
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return Optional.of(dot < 0 ? rest : rest.substring(0, dot));
    }
}
