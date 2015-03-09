package org.jboss.ddoyle.drools.demo;

import static org.jboss.ddoyle.drools.demo.KieTestUtils.createKieJar;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jboss.ddoyle.drools.demo.listener.RulesFiredAgendaEventListener;
import org.jboss.ddoyle.drools.demo.model.v1.Event;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

/**
 * Tests incremental update of KieBase and KieSession with accumulates. 
 * 
 * @author <a href="mailto:duncan.doyle@redhat.com">Duncan Doyle</a>
 */
public class KieSessionRulesIncrementalUpdateAccumulateTest {

	/**
	 * Tests updating the KieBase with a single accumulate by reloading the KieSession with the same rule file. Note that after the
	 * test-run, the rule has been fired 6 times, 1 on insert of SimpleEvent-1, 2 times on insert of SimpleEvent-2 (once for SimpleEvent-1 +
	 * new Accumulate value and once for SimpleEvent-2 and new Accumulate value) and 3 times on insert of SimpleEvent 3 (once for
	 * SimplEvent-1 + new Accumulate value, once for SimpleEvent-2 and new Accumulate value and once for SimpleEvent-3 and new accumulate
	 * value).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRulesAccumulate() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-accumulate-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("accumulateRules/original-rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();

		AccumulateCount accCount = new AccumulateCount();

		kieSession.setGlobal("accCount", accCount);

		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));

			assertEquals(2, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId,
					kieServices.getResources().newClassPathResource("accumulateRules/original-rules.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(6, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));

			// Expect 6 because, when we add a single event, the rule will fire for all 3 events (because the accumulate gets a new value
			// because an entry has been added.).
			assertEquals(3, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by changing the name of a rule with a single accumulate node. Notice that the rule is marked as a new rule
	 * and will fire '3' (!!!) times, once for each event/fact, BUT NOT 6 times. I.e. the accumulate memory is not re-created, so the
	 * accumulate will only create a single fact/event.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenamedRuleAccumulate() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-renamed-accumulate-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("accumulateRules/original-rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();

		AccumulateCount accCount = new AccumulateCount();

		kieSession.setGlobal("accCount", accCount);

		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));

			assertEquals(2, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId,
					kieServices.getResources().newClassPathResource("accumulateRules/renamed-rules.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			/*
			 * The renamed rule will only fire 3 times, ones for each event. Note that it doesn't do the 'full-cycle' of inserting things,
			 * accumulate, etc. If that would have been the case, the rule would have fired 6 times (because it would rebuild the
			 * accumulate, which would cause the rules to fire 1 (insert of SimpleEvent 1) + 2 (insert of SimpleEvent 2) + 3 (insert of
			 * SimpleEvent 3) = 6 times
			 */
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "Bla"));

			// Expect 3 facts in the session. So accumulate count should have counted 3 simple events.
			assertEquals(3, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Testing a more complex accumulate to verify how the memort is preserved on a KieSession update.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testComplexAccumulate() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-complex-accumulate-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("accumulateRules/complex-accumulate-rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();

		AccumulateCount accCount = new AccumulateCount();

		kieSession.setGlobal("accCount", accCount);

		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));

			assertEquals(2, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId,
					kieServices.getResources().newClassPathResource("accumulateRules/complex-accumulate-rules.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(6, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));

			// Expect 3 facts in the session. So accumulate count should have counted 3 simple events.
			assertEquals(3, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Tests updating the KieBase by changing the name of a rule with a single accumulate node. Notice that this rule will be marked as new
	 * rule. However, the accumulate will only create a single event/fact on reload, the one that's created because of the added event. So
	 * even though the rule is marked as a new rule, it will not re-fire for the first 2 events.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenamedRule2Accumulate() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-renamed-accumulate-rules-same-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("accumulateRules/original-rules-2.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();

		AccumulateCount accCount = new AccumulateCount();

		kieSession.setGlobal("accCount", accCount);

		try {
			RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
			kieSession.addEventListener(rulesFiredListener);

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();

			for (Event nextEvent : firstEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));

			assertEquals(2, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId,
					kieServices.getResources().newClassPathResource("accumulateRules/renamed-rules-2.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			/*
			 * The renamed rule will only fire 1 times, only for the added event. Note that, in rules that don't use these 'from accumulate'
			 * constructs, the rule would be marked as a new rule and thus fire 3 times, i.e. for every inserted Event. Here however, the
			 * accumulate node seems to be updated only once (only for the new event), and thus the rule will fire only once.
			 */
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "Bla"));

			// Expect 3 facts in the session. So accumulate count should have counted 3 simple events.
			assertEquals(3, ((AccumulateCount) kieSession.getGlobal("accCount")).getValue());

		} finally {
			kieSession.dispose();
		}
	}

	public static class AccumulateCount {

		private int value = 0;

		public AccumulateCount() {
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

}
