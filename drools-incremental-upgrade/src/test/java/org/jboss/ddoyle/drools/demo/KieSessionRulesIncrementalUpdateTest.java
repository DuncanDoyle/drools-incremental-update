package org.jboss.ddoyle.drools.demo;

import static org.jboss.ddoyle.drools.demo.KieTestUtils.createKPom;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.createKPomBytes;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.getPom;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.insertAndAdvance;
import static org.jboss.ddoyle.drools.demo.KieTestUtils.logRulesInKieBase;
import static org.kie.scanner.MavenRepository.getMavenRepository;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.drools.compiler.CommonTestMethodBase;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jboss.ddoyle.drools.demo.listener.RulesFiredAgendaEventListener;
import org.jboss.ddoyle.drools.demo.model.v1.SimpleEvent;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.scanner.MavenRepository;

public class KieSessionRulesIncrementalUpdateTest extends CommonTestMethodBase {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmmssSSS");

	private static final String KMODULE_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<kmodule xmlns=\"http://jboss.org/kie/6.0.0/kmodule\">"
			+ "<kbase name=\"rules\" equalsBehavior=\"equality\" eventProcessingMode=\"stream\" default=\"true\">"
			+ "<ksession name=\"ksession-rules\" default=\"true\" type=\"stateful\" clockType=\"pseudo\"/>" + "</kbase>" + "</kmodule>";

	/**
	 * Deploys a KJAR, inserts a number of facts and fires rules. Next uses the KieScanner to do an upgrade of the KieBase (in the running
	 * session). New facts are inserted, rules are fired and expected results asserted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddedRulesWithoutScanner() throws Exception {

		final KieServices ks = KieServices.Factory.get();
		final MavenRepository repository = getMavenRepository();

		final ReleaseId releaseId = ks.newReleaseId("org.kie", "scanner-test-1", "1.0-SNAPSHOT");

		CreateAndDeployKJarCallback createAndDeployKJarCallback1 = new CreateAndDeployKJarCallback() {
			@Override
			public void createAndDeployKJar() throws Exception {
				Resource originalRulesDrlResource = ks.getResources().newClassPathResource("originalRules/rules.drl");
				InternalKieModule kJar1 = createKieJar(ks, releaseId, originalRulesDrlResource);
				repository.deployArtifact(releaseId, kJar1, createKPom(releaseId));
			}
		};

		CreateAndDeployKJarCallback createAndDeployKJarCallback2 = new CreateAndDeployKJarCallback() {
			@Override
			public void createAndDeployKJar() throws Exception {

				Resource addedRulesDrlResource = ks.getResources().newClassPathResource("addedRules/rules.drl");
				InternalKieModule kJar2 = createKieJar(ks, releaseId, addedRulesDrlResource);
				repository.deployArtifact(releaseId, kJar2, createKPom(releaseId));
			}
		};

		runTest(createAndDeployKJarCallback1, createAndDeployKJarCallback2, releaseId);
	}

	
	/**
	 * Same as the previous test but uses a different mechanism to create the KJAR. Should give the same result as above, but doesn't. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddedRulesWithoutScanner2() throws Exception {

		final KieServices ks = KieServices.Factory.get();
		final MavenRepository repository = getMavenRepository();

		final ReleaseId releaseId = ks.newReleaseId("org.kie", "scanner-test-2", "1.0-SNAPSHOT");

		CreateAndDeployKJarCallback createAndDeployKJarCallback1 = new CreateAndDeployKJarCallback() {
			@Override
			public void createAndDeployKJar() throws Exception {
				Resource originalRulesDrlResource = ks.getResources().newClassPathResource("originalRules/rules.drl");
				byte[] kJar1 = createJar(ks, KMODULE_CONTENT, releaseId, originalRulesDrlResource);
				repository.deployArtifact(releaseId, kJar1, createKPomBytes(releaseId));
			}
		};

		CreateAndDeployKJarCallback createAndDeployKJarCallback2 = new CreateAndDeployKJarCallback() {
			@Override
			public void createAndDeployKJar() throws Exception {
				Resource addedRulesDrlResource = ks.getResources().newClassPathResource("addedRules/rules.drl");
				byte[] kJar2 = createJar(ks, KMODULE_CONTENT, releaseId, addedRulesDrlResource);
				repository.deployArtifact(releaseId, kJar2, createKPomBytes(releaseId));
			}
		};

		runTest(createAndDeployKJarCallback1, createAndDeployKJarCallback2, releaseId);

	}

	private void runTest(CreateAndDeployKJarCallback createAndDeployKJar1, CreateAndDeployKJarCallback createAndDeployKJar2,
			ReleaseId releaseId) throws Exception {
		KieServices ks = KieServices.Factory.get();

		// Create and deploy the first KJar.
		createAndDeployKJar1.createAndDeployKJar();

		KieContainer kieContainer = ks.newKieContainer(releaseId);
		
		KieSession kieSession = kieContainer.newKieSession();

		logRulesInKieBase(kieSession.getKieBase());

		RulesFiredAgendaEventListener rulesFiredListener = new RulesFiredAgendaEventListener();
		kieSession.addEventListener(rulesFiredListener);

		List<SimpleEvent> firstEvents = getFirstSimpleEvents();
		for (SimpleEvent nextEvent : firstEvents) {
			insertAndAdvance(kieSession, nextEvent);
			kieSession.fireAllRules();
		}
		assertEquals(2, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
		assertEquals(0, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));

		// Create and deploy the second KJar.
		createAndDeployKJar2.createAndDeployKJar();

		kieContainer.updateToVersion(releaseId);
		
		logRulesInKieBase(kieSession.getKieBase());

		List<SimpleEvent> secondEvents = getSecondSimpleEvents();
		for (SimpleEvent nextEvent : secondEvents) {
			insertAndAdvance(kieSession, nextEvent);
			kieSession.fireAllRules();
		}

		assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-One"));
		assertEquals(1, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Two"));
		// This rule will fire for ALL EVENTS in the session. Not only the ones we inserted after updating the KieContainer.
		assertEquals(3, rulesFiredListener.getNrOfRulesFired("org.jboss.ddoyle.drools.cep.sample" + "-" + "SimpleTestRule-Three"));
	}

	private abstract class CreateAndDeployKJarCallback {
		public abstract void createAndDeployKJar() throws Exception;
	}

	private List<SimpleEvent> getFirstSimpleEvents() throws Exception {
		List<SimpleEvent> simpleEvents = new ArrayList<>();
		simpleEvents.add(new SimpleEvent("1", "MY_CODE", DATE_FORMAT.parse("20150223:090000000")));
		simpleEvents.add(new SimpleEvent("2", "MY_CODE", DATE_FORMAT.parse("20150223:090005000")));
		return simpleEvents;
	}

	private List<SimpleEvent> getSecondSimpleEvents() throws Exception {
		List<SimpleEvent> simpleEvents = new ArrayList<>();
		simpleEvents.add(new SimpleEvent("3", "MY_CODE", DATE_FORMAT.parse("20150223:090021000")));
		return simpleEvents;
	}

	private InternalKieModule createKieJar(KieServices ks, ReleaseId releaseId, Resource resource) {
		KieFileSystem kfs = createKieFileSystemWithKProject(ks, true);
		kfs.writePomXML(getPom(releaseId));

		kfs.write("src/main/resources/rules.drl", resource);

		KieBuilder kieBuilder = ks.newKieBuilder(kfs);

		assertTrue("", kieBuilder.buildAll().getResults().getMessages().isEmpty());
		return (InternalKieModule) kieBuilder.getKieModule();
	}

	protected KieFileSystem createKieFileSystemWithKProject(KieServices ks, boolean isdefault) {
		KieModuleModel kproj = ks.newKieModuleModel();

		KieBaseModel kieBaseModel1 = kproj.newKieBaseModel("KBase1").setDefault(isdefault)
				.setEqualsBehavior(EqualityBehaviorOption.EQUALITY).setEventProcessingMode(EventProcessingOption.STREAM);

		// Configure the KieSession.
		kieBaseModel1.newKieSessionModel("KSession1").setDefault(isdefault).setType(KieSessionModel.KieSessionType.STATEFUL)
				.setClockType(ClockTypeOption.get("pseudo"));

		KieFileSystem kfs = ks.newKieFileSystem();
		kfs.writeKModuleXML(kproj.toXML());
		return kfs;
	}

}
