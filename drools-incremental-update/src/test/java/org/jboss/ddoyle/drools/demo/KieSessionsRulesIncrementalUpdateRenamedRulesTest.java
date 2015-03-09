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

public class KieSessionsRulesIncrementalUpdateRenamedRulesTest {

	/**
	 * Tests updating the KieBase by changing the name of rule one. This marks the rule as a new rule and will thus re-fire the first rule
	 * on KieSession incremental update for the events that were already in the session.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenamedRuleOne() throws Exception {

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

			// Add new KJAR and update the KieContainer to incrementally update the KieSession. Changed name of first rule.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("renamedRules/rules-1.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "Bla"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "Bla"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by changing the name of rule twi. This marks the rule as a new rule and will thus re-fire for matches
	 * created for facts/events that were already in the KieSession before the incremental update.
	 * 
	 * In this case however, rule-2 did not yet fire before the update.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenamedRuleTwo() throws Exception {

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

			// Add new KJAR and update the KieContainer to incrementally update the KieSession. Changed name of first rule.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("renamedRules/rules-2.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "Bla"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "Bla"));

		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by changing the name of rule twi. This marks the rule as a new rule and will thus re-fire for matches
	 * created for facts/events that were already in the KieSession before the incremental update.
	 * 
	 * In this case however, rule-2 did fire before the update. The renamed rule will fire again for the same facts already in WM before the update!
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenamedRuleTwo2() throws Exception {

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

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Add new KJAR and update the KieContainer to incrementally update the KieSession. Changed name of first rule.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("renamedRules/rules-2.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);
			kieSession.fireAllRules();

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "Bla"));

		} finally {
			kieSession.dispose();
		}
	}

}
