package app.lyra.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;

final class HexagonalRules {

    private HexagonalRules() {
    }

    static ArchRule domainDependsOnlyOnJdkAndDomain(String root) {
        return classes().that().resideInAPackage(root + ".*.domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "lombok..", root + ".*.domain..");
    }

    static ArchRule applicationDoesNotDependOnAdapters(String root) {
        return noClasses().that().resideInAPackage(root + ".*.application..")
                .should().dependOnClassesThat().resideInAPackage(root + ".*.adapter..");
    }

    static ArchRule inboundAdaptersDoNotDependOnOutboundAdapters(String root) {
        return noClasses().that().resideInAPackage(root + ".*.adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(root + ".*.adapter.out..");
    }

    static ArchRule outboundAdaptersDoNotDependOnInboundAdapters(String root) {
        return noClasses().that().resideInAPackage(root + ".*.adapter.out..")
                .should().dependOnClassesThat().resideInAPackage(root + ".*.adapter.in..");
    }

    static ArchRule modulesTalkOnlyThroughInboundPortsAndDomain(String root) {
        return modules().definedByPackages(root + ".(*)..")
                .should().onlyDependOnEachOtherThroughClassesThat()
                .resideInAnyPackage("..application.port.in..", "..domain..");
    }
}
