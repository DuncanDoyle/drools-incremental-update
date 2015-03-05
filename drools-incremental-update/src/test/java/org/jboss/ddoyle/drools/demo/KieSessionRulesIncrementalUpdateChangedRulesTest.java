package org.jboss.ddoyle.drools.demo;

import static org.jboss.ddoyle.drools.demo.KieTestUtils.createKieJar;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.jboss.ddoyle.drools.demo.listener.RulesFiredAgendaEventListener;
import org.jboss.ddoyle.drools.demo.model.v1.Event;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieSessionRulesIncrementalUpdateChangedRulesTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(KieSessionRulesIncrementalUpdateChangedRulesTest.class);

	/**
	 * Tests updating the KieBase by changing a rule in the same DRL. In this test we add an additional constraint in the LHS which defines
	 * the rule as a NEW rule. The constraint will only match a newly inserted event, and thus rule 1 and rule 2 will both fire once after
	 * the KieBase update.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangedRules1SameDrl() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("originalRules/rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("changedRules/rules-1.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			// The third event will match with the first rule, as its id == 3.
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by changing a rule and changing the resource name. In this test we add an additional constraint in the LHS
	 * which defines the rule as a NEW rule. At the same time, because the rules are defined in a different DRL, all rules are actually
	 * marked as new rules, and will thus re-fire for all Events. However, due that the update now only creates a Match and activation for
	 * the third event, which we insert after the KieBase update, the rule will not refire for events 1 and 2.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangedRules1DifferentDrl() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId, new KieTestUtils.ResourceWrapper(kieServices.getResources()
				.newClassPathResource("originalRules/rules.drl"), "originalRules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId, new KieTestUtils.ResourceWrapper(kieServices.getResources()
					.newClassPathResource("changedRules/rules-1.drl"), "changedRules.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			// The first event will match AGAIN with the first rule as its LHS has changed.
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by changing a rule by changing the LHS in the same DRL. In this test we add a constraint that cause the
	 * 3rd event not to match and create an activation. However, because we change the LHS, the rule is marked as a new rule, and will
	 * refire for event-1.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangedRules2SameDrl() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("originalRules/rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("changedRules/rules-2.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			// The first event will re-match with the first rule, as the LHS of the rule, and thus the rule itself has changed.
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by changing a rule by changing the LHS in the same DRL. This is the same test as
	 * {@link #testChangedRules2SameDrl()}, we've only changed the order of the constraints. Same result is expected, because of the change
	 * in the LHS, the rule is marked as a new rule and will thus refire on a match.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangedRules3SameDrl() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("originalRules/rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("changedRules/rules-3.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			// The first event will re-match with the first rule, as the LHS of the rule, and thus the rule itself has changed.
			// The rule doesn't match the 3rd event.
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase changing the RHS of an existing rule. This will categorize the rule as a NEW rule, and thus will refire
	 * for events already in the WM.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangedRules4SameDrl() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("originalRules/rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("changedRules/rules-4.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			// The third event will match, the first and second WILL REMATCH, as the RHS has changed WHICH CLASSIEFIES AS A RULE-CHANGE!
			assertEquals(5, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);

			kieSession.fireAllRules();

			assertEquals(5, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase changing the order of the constraints in the LHS. This will mark the rule as changed, and thus will refire
	 * for events in the WM that match.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangedRulesLhsConstraintOrderSameDrl() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("changedRules/rules-2.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			// Only the first event will match.
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("changedRules/rules-3.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			// The first event will REMATCH because we changed the order of constraints in the LHS! Therefore, the rule is marked as a new
			// rule.
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);

			kieSession.fireAllRules();

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}
	}

}
