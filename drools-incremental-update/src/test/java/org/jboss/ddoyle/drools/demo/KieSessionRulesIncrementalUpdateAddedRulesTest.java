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

public class KieSessionRulesIncrementalUpdateAddedRulesTest {

	/**
	 * Tests updating the KieBase by adding a new rule to the same DRL file. This should NOT re-fire the first rule on KieSession
	 * incremental update for the events that were already in the session.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddedRulesSameDrl() throws Exception {

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
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("addedRules/rules.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Three"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Three"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by adding a new rule to a different DRL file. Because the resource (DRL) is different, all rules are
	 * treated as new rules. This will cause rule 1 to re-fire for events still in the session. In other words, if the rules are in a
	 * new/different resource when updating the KieContainer, they are treated as new rules.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddedRulesDifferentDrl() throws Exception {

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
			// kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("addedRules/rules.drl"));
			kieModule = createKieJar(kieServices, releaseId, new KieTestUtils.ResourceWrapper(kieServices.getResources()
					.newClassPathResource("addedRules/rules.drl"), "addedRules.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			/* 
			 * !!!! The first rule will fire again for the events that were already in the session. This is because the rule is seen as a new rule, as it is defined in a different resource (DRL).
			 */
			assertEquals(5, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Three"));

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();

			assertEquals(5, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Three"));
		} finally {
			kieSession.dispose();
		}

	}

}
