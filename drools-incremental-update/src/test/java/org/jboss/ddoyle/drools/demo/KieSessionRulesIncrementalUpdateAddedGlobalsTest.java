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

public class KieSessionRulesIncrementalUpdateAddedGlobalsTest {

	@Test
	public void testAddedGlobal() throws Exception {
		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-added-global-drl", "1.0.0");

		InternalKieModule kieModule = createKieJar(kieServices, releaseId,
				kieServices.getResources().newClassPathResource("originalRules/rules.drl"));
		kieServices.getRepository().addKieModule(kieModule);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {

			boolean caughtExpectedException = false;
			try {
				kieSession.setGlobal("myNewGlobal", "BLAA!");
			} catch(RuntimeException re) {
				caughtExpectedException = true;
			}
			assertEquals(true, caughtExpectedException);
			
			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			kieModule = createKieJar(kieServices, releaseId, kieServices.getResources().newClassPathResource("addedGlobals/rules.drl"));
			kieServices.getRepository().addKieModule(kieModule);
			kieContainer.updateToVersion(releaseId);

			kieSession.setGlobal("myNewGlobal", "BLAA!");

			((PseudoClockScheduler) kieSession.getSessionClock()).advanceTime(12, TimeUnit.SECONDS);
			kieSession.fireAllRules();
			
		} finally {
			kieSession.dispose();
		}
	}

}
