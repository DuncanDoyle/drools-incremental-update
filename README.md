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
We will now explain the behaviour of the incremental update of rules in a running `KieSession`.

### Refactoring DRL name and/or folder
* Drools will treat rules in a new DRL file as new rules, even if only the DRL file was renamed or replaced (e.g. stored in a different package/folder). This is demonstrated in [these tests](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateDifferentDrlTest.java).
### Adding rules
* When a `KieBase` is changed by adding a new rule to an existing DRL file (e.g. not changing the name of the DRL), the existing rules will **not** refire for facts/events that are in the KieSession. The new rule however will fire for **all** facts/events that are already in the KieSession and that match the rule. E.g. if you have inserted 2 facts/events into the `KieSession`, and after that you add a rule that creates a match for both fatcs/events, the new rule will fire twice on the next call to `KieSession.fireAllRules()`. Existing rules will not re-fire. These tests can be found [here](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateAddedRulesTest.java#L28).
* When a 'KieBase' is changed by adding a new rule **and** changing the name of the DRL file, the existing rules **and** the new rules will (re)fire for all facts/evens in the KieSession (see this [test](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateAddedRulesTest.java#L89)).
### Changing Rules
* When a rule is changed by changing the LHS of a rule (adding constraints, removing constrains, re-ordering constraints), the rule is marked as *new*. I.e. the rule will refire for facts/events that were in Working Memory before the `KieSession` was updated. This is tested [here](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateChangedRulesTest.java).
* When a rule is changed bu changing the RHS of a rule, the rule is marked as *new* and the rule will refire for facts/events that were already in Working Memory before the `KieSession` was updated, as shown in [this test](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateChangedRulesTest.java#L272).
### Renaming Rules
* When a rule is remamed, the rule is treated as a *new* rule and will thus refire for facts/events that were already in WorkingMemory before the update. This is tested [here](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateRenamedRulesTest.java).
### Deleting rules
* When a rule is deleted, remaining rules will **not** (re)fire for facts/events that were already in Working Memory before the `KieSession` update, as demonstrated [here](https://github.com/DuncanDoyle/drools-incremental-update/blob/master/drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateDeletedRulesTest.java).


## Interesting links:
* [The Drools project](http://www.drools.org)
* [The JBoss BRMS platform](http://www.redhat.com/en/technologies/jboss-middleware/business-rules)

