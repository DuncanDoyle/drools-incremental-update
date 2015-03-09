# Drools Incremental KieBase/KieSession Update PoC

This small Proof of Concept shows the **JBoss BRMS/Drools** KieBase/KieSession incremental update feature.

## Incremental Updates
Drools provides the functionality to incrementally update the KieBase **and** running KieSessions created from that KieBase.
What this means is that Drools allows one to, at runtime, update the rules in a KieBase without having to restart the application.
It even allows to update rules in (long-)running KieSessions, which, for example, might be used in Complex Event Processign (CEP) scenario's.
This functionality is provided by the `updateToVersion(ReleaseId version)` method of the `KieContainer`. This functionality does
not explicitly require a `KieScanner` to be used, the functionality can also be used by directly using the `KieContainer` API,
as is shown in the unit-tests of this PoC.

## The PoC
This PoC is quite basic. It provides a (simple) domain/fact model, consisting of a single fact called [SimpleEvent](drools-incremental-update/src/main/java/org/jboss/ddoyle/drools/demo/model/v1/SimpleEvent.java).
This model, in combination with the various Drools *drl* rule defintion files, is used in the various JUnit tests that test the diffferent scenarios. Each JUnit test tests a different scenario, for example 
adding rules, changing rules, deleting rules, etc. These tests can be found in the [src/test/java](drools-incremental-update/src/test/java) directory. All tests contain a small JavaDoc comment that explains
the purpose of the test and the expected semantics and outcome.

To run the tests, execute `mvn clean test` in the root directory of the project.

## The incremental update semantics




## Interesting links:
* [The Drools project](http://www.drools.org)
* [The JBoss BRMS platform](http://www.redhat.com/en/technologies/jboss-middleware/business-rules)

