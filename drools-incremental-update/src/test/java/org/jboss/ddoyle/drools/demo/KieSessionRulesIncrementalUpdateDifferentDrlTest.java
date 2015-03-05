package org.jboss.ddoyle.drools.demo;

import static org.jboss.ddoyle.drools.demo.KieTestUtils.createKieJar;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.insertAndAdvance;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.logRulesInKieBase;

import java.util.List;

import org.drools.compiler.CommonTestMethodBase;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jboss.ddoyle.drools.demo.listener.RulesFiredAgendaEventListener;
import org.jboss.ddoyle.drools.demo.model.v1.SimpleEvent;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class KieSessionRulesIncrementalUpdateDifferentDrlTest extends CommonTestMethodBase {

	/**
	 * Tests incremental KieBase and KieSession update where rules are added in the same DRL. Expect that unchanged rules will NOT refire
	 * for events that are already in WM.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddedRulesInSameDrl() throws Exception {
		KieServices kieServices = KieServices.Factory.get();

		final ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-different-drl", "1.0.0");

		// Create and deploy the first KJar.
		InternalKieModule kJar = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("originalRules/rules.drl"));
		kieServices.getRepository().addKieModule(kJar);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {

		logRulesInKieBase(kieSession.getKieBase());

		RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
		kieSession.addEventListener(rulesFiredListener);

		List<SimpleEvent> firstEvents = TestEventsFactory.getFirstSimpleEvents();
		for (SimpleEvent nextEvent : firstEvents) {
			insertAndAdvance(kieSession, nextEvent);
			kieSession.fireAllRules();
		}
		assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
		assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

		kJar = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("addedRules/rules.drl"));
		kieServices.getRepository().addKieModule(kJar);
		kieContainer.updateToVersion(releaseId);

		logRulesInKieBase(kieSession.getKieBase());

		List<SimpleEvent> secondEvents = TestEventsFactory.getSecondSimpleEvents();
		for (SimpleEvent nextEvent : secondEvents) {
			insertAndAdvance(kieSession, nextEvent);
			kieSession.fireAllRules();
		}

		assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
		assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		// This rule will fire for ALL EVENTS in the session. Not only the ones we inserted after updating the KieContainer.
		assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Three"));
		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Same test as above in {@link KieSessionRulesIncrementalUpdateDifferentDrlTest#testAddedRulesInSameDrl()}, however, in this test we
	 * define the rules in a different DRL file, which marks all rules as new. This means that unchanged rules (in syntax), will still
	 * re-fire because they have been defined in a new DRL resource.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddedRulesInDifferentDrl() throws Exception {
		KieServices kieServices = KieServices.Factory.get();

		final ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-rules-different-drl", "1.0.0");

		// Create and deploy the first KJar.
		InternalKieModule kJar = createKieJar(kieServices, releaseId, new KieTestUtils.ResourceWrapper(kieServices.getResources()
				.newClassPathResource("originalRules/rules.drl"), "originalRules.drl"));
		kieServices.getRepository().addKieModule(kJar);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {

		logRulesInKieBase(kieSession.getKieBase());

		RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
		kieSession.addEventListener(rulesFiredListener);

		List<SimpleEvent> firstEvents = TestEventsFactory.getFirstSimpleEvents();
		for (SimpleEvent nextEvent : firstEvents) {
			insertAndAdvance(kieSession, nextEvent);
			kieSession.fireAllRules();
		}
		assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
		assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

		kJar = createKieJar(kieServices, releaseId,
				new KieTestUtils.ResourceWrapper(kieServices.getResources().newClassPathResource("addedRules/rules.drl"), "addedRules.drl"));
		kieServices.getRepository().addKieModule(kJar);
		kieContainer.updateToVersion(releaseId);

		logRulesInKieBase(kieSession.getKieBase());

		List<SimpleEvent> secondEvents = TestEventsFactory.getSecondSimpleEvents();
		for (SimpleEvent nextEvent : secondEvents) {
			insertAndAdvance(kieSession, nextEvent);
			kieSession.fireAllRules();
		}

		// Because the rules are defined in a different Resource, they are marked as new rules and will re-activate and refire for events
		// already in the session.
		assertEquals(5, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
		assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		// This rule will fire for ALL EVENTS in the session. Not only the ones we inserted after updating the KieContainer.
		assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Three"));
		} finally {
			kieSession.dispose();
		}
		
		
	}

}
