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

public class KieSessionRulesOriginalRulesTest {

	/**
	 * Tests the original rules.
	 */
	@Test
	public void testOriginalRules() throws Exception {

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-original-rules", "1.0.0");

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
			
			
			List<? extends Event> secondEvents = TestEventsFactory.getSecondSimpleEvents();

			for (Event nextEvent : secondEvents) {
				KieTestUtils.insertAndAdvance(kieSession, nextEvent);
				kieSession.fireAllRules();
			}
			
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

}
