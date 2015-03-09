package org.jboss.ddoyle.drools.demo;

import static org.jboss.ddoyle.drools.demo.KieTestUtils.createKieJar;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.insertAndAdvance;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.logRulesInKieBase;

import java.util.List;

import org.drools.compiler.CommonTestMethodBase;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jboss.ddoyle.drools.demo.listener.RulesFiredAgendaEventListener;
import org.jboss.ddoyle.drools.demo.model.v1.Event;
import org.jboss.ddoyle.drools.demo.model.v1.SimpleEvent;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

/**
 * Tests use-cases where the DRL file is renamed and/or replaced. Drools will mark all rules in renamed and/or replaced DRL files as new
 * rules, and will thus (re)fire these rules for all facts/events in WorkingMemory, including the ones inserted before the
 * {@link KieSession} update.
 * 
 * @author <a href="mailto:duncan.doyle@redhat.com">Duncan Doyle</a>
 */
public class KieSessionRulesIncrementalUpdateDifferentDrlTest extends CommonTestMethodBase {

	/**
	 * Tests incremental KieBase and KieSession update where the same DRL is reloaded (without any changes). Expect that unchanged rules
	 * will NOT refire for events that were already in WM before the {@link KieSession} update.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRulesInSameDrl() throws Exception {
		KieServices kieServices = KieServices.Factory.get();

		final ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-rules-same-drl", "1.0.0");

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

			List<? extends Event> firstEvents = TestEventsFactory.getFirstSimpleEvents();
			for (Event nextEvent : firstEvents) {
				insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

			// Just adding the same rules.drl with the same name, so nothing should change and no additional rules should fire.
			kJar = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("originalRules/rules.drl"));
			kieServices.getRepository().addKieModule(kJar);
			kieContainer.updateToVersion(releaseId);

			logRulesInKieBase(kieSession.getKieBase());

			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();
			for (Event nextEvent : secondEvents) {
				insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			// Rules will only fire for new events.
			assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

		} finally {
			kieSession.dispose();
		}
	}

	/**
	 * Same test as above in {@link KieSessionRulesIncrementalUpdateDifferentDrlTest#testRulesInSameDrl()}, however, in this test we define
	 * the rules in a different DRL file, which marks all rules as new. This means that unchanged rules (in syntax), will still re-fire
	 * because they have been defined in a new DRL resource.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRulesInDifferentDrl() throws Exception {
		KieServices kieServices = KieServices.Factory.get();

		final ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-rules-different-drl", "1.0.0");

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
					new KieTestUtils.ResourceWrapper(kieServices.getResources().newClassPathResource("originalRules/rules.drl"),
							"newOriginalRules.drl"));
			kieServices.getRepository().addKieModule(kJar);
			kieContainer.updateToVersion(releaseId);

			logRulesInKieBase(kieSession.getKieBase());

			List<SimpleEvent> secondEvents = TestEventsFactory.getSecondSimpleEvents();
			for (SimpleEvent nextEvent : secondEvents) {
				insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			/*
			 * Because the rules are defined in a different Resource, they are marked as new rules and will re-activate and refire for all
			 * events, also the ones that were already in the session before the KieBase/KieSession update.
			 */
			assertEquals(5, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}

	}

	/**
	 * Same test as above in {@link KieSessionRulesIncrementalUpdateDifferentDrlTest#testRulesInSameDrl()}, however, in this test we define
	 * the rules in the same DRL file, but in a different path, which also marks all rules as new. This means that unchanged rules (in syntax), will still re-fire
	 * because they have been defined in a new DRL resource.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRulesInDifferentDrlPath() throws Exception {
		KieServices kieServices = KieServices.Factory.get();

		final ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-rules-different-drl", "1.0.0");

		// Create and deploy the first KJar.
		InternalKieModule kJar = createKieJar(kieServices, releaseId, new KieTestUtils.ResourceWrapper(kieServices.getResources()
				.newClassPathResource("originalRules/rules.drl"), "firstPath/originalRules.drl"));
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
					new KieTestUtils.ResourceWrapper(kieServices.getResources().newClassPathResource("originalRules/rules.drl"),
							"secondPath/originalRules.drl"));
			kieServices.getRepository().addKieModule(kJar);
			kieContainer.updateToVersion(releaseId);

			logRulesInKieBase(kieSession.getKieBase());

			List<SimpleEvent> secondEvents = TestEventsFactory.getSecondSimpleEvents();
			for (SimpleEvent nextEvent : secondEvents) {
				insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}

			/*
			 * Because the rules are defined in a different Resource, they are marked as new rules and will re-activate and refire for all
			 * events, also the ones that were already in the session before the KieBase/KieSession update.
			 */
			assertEquals(5, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
			assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		} finally {
			kieSession.dispose();
		}

	}

}
