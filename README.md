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
* When a rule is deleted, remaining rules will **not** (re)fire for facts/events that were already in Working Memory before the `KieSession` update, as demonstrated [here](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateDeletedRulesTest.java).

### Rules with accumulates
* The [following tests](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateAccumulateTest.java) test KieBase and KieSession update semantics with rules containing accumulates. There are some interesting conclusions:
    - [This test](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateAccumulateTest.java#L34) shows that the rule is called 6 times, once when SimpleEvent-1 is inserted, twice when SimpleEvent-2 is inserted (once for SimpleEvent-1 and new Accumulate value and once for SimpleEvent-2 and new Accumulate Value) and 3 times when SimpleEvent-3 is inserted (once for SimpleEvent-1 and Accumulate value 3, once for SimpleEvent-2 and Accumulate value 3 and once for SimpleEvent-3 and Accumulate value 3).
    - [This test](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateAccumulateTest.java#L97) shows that when we rename the rule, although the rule is marked as a new rule, 
the renamed rule is fired only 3 times and not 6 times. This is because the accumulate only creates a single new fact.
    - [This test](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateAccumulateTest.java#L164) shows a complex accumulate and tests how the accumulate memory is preserved during the update of a KieSession.
    - [This test](drools-incremental-update/src/test/java/org/jboss/ddoyle/drools/demo/KieSessionRulesIncrementalUpdateAccumulateTest.java#L226) shows that a renamed rule with only a single accumulate is, although a renamed rule is marked as a *new* rule, the renamed rule is only fired once, as the accumulate only generates a single new fact.

## Interesting links:
* [The Drools project](http://www.drools.org)
* [The JBoss BRMS platform](http://www.redhat.com/en/technologies/jboss-middleware/business-rules)

