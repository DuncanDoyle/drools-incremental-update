package org.jboss.ddoyle.drools.demo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.core.common.InternalAgenda;
import org.drools.core.spi.Activation;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.drools.core.util.FileManager;
import org.jboss.ddoyle.drools.demo.model.v1.Event;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;

public class KieTestUtils {
	
	private static final FileManager fileManager = new FileManager();
	
	static {
		fileManager.setUp();
	}

	public static void insertAndAdvance(KieSession kieSession, Event event) {
		kieSession.insert(event);
		// Advance the clock if required.
		PseudoClockScheduler clock = kieSession.getSessionClock();
		long advanceTime = event.getTimestamp().getTime() - clock.getCurrentTime();
		if (advanceTime > 0) {
			clock.advanceTime(advanceTime, TimeUnit.MILLISECONDS);
		}
	}
	
	public static void logRulesInKieBase(KieBase kieBase) {
		Collection<KiePackage> kiePackages = kieBase.getKiePackages();
		for (KiePackage nextKiePackage:kiePackages) {
			Collection<Rule> rules = nextKiePackage.getRules();
			for (Rule nextRule:rules) {
				System.out.println("Rule: " + nextRule.getPackageName() + "-" + nextRule.getName());
			}
		}
	}
	
	public static  KieFileSystem createKieFileSystemWithKProject(KieServices ks, boolean isdefault) {
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
	
	
	public static File createKPom(ReleaseId releaseId, ReleaseId... dependencies) throws IOException {
		File pomFile = fileManager.newFile("pom.xml");
		fileManager.write(pomFile, getPom(releaseId, dependencies));
		return pomFile;
	}
	
	public static byte[] createKPomBytes(ReleaseId releaseId, ReleaseId... dependencies) throws IOException {
			return getPom(releaseId, dependencies).getBytes();
	}
 	
	public static String getPom(ReleaseId releaseId, ReleaseId... dependencies) {
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                        "  <modelVersion>4.0.0</modelVersion>\n" +
                        "\n" +
                        "  <groupId>" + releaseId.getGroupId() + "</groupId>\n" +
                        "  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" +
                        "  <version>" + releaseId.getVersion() + "</version>\n" +
                        "\n";
        if (dependencies != null && dependencies.length > 0) {
            pom += "<dependencies>\n";
            for (ReleaseId dep : dependencies) {
                pom += "<dependency>\n";
                pom += "  <groupId>" + dep.getGroupId() + "</groupId>\n";
                pom += "  <artifactId>" + dep.getArtifactId() + "</artifactId>\n";
                pom += "  <version>" + dep.getVersion() + "</version>\n";
                pom += "</dependency>\n";
            }
            pom += "</dependencies>\n";
        }
        pom += "</project>";
        return pom;
    }
	
	
	public static InternalKieModule createKieJar(KieServices ks, ReleaseId releaseId, Resource resource) {
		return createKieJar(ks, releaseId, new ResourceWrapper(resource,"rules.drl"));
	}
	
	public static InternalKieModule createKieJar(KieServices ks, ReleaseId releaseId, ResourceWrapper resourceWrapper) {
		KieFileSystem kfs = createKieFileSystemWithKProject(ks, true);
		kfs.writePomXML(getPom(releaseId));

		kfs.write("src/main/resources/" + resourceWrapper.targetResourceName, resourceWrapper.getResource());

		KieBuilder kieBuilder = ks.newKieBuilder(kfs);

		if (!kieBuilder.buildAll().getResults().getMessages().isEmpty()) {
			throw new IllegalStateException("Error creating KieBuilder.");
		}
		
		return (InternalKieModule) kieBuilder.getKieModule();
	}
	
	
	public static class ResourceWrapper {
		
		private final Resource resource;
		
		private final String targetResourceName;
		
		public ResourceWrapper(Resource resource, String targetResourceName) {
			this.resource = resource;
			this.targetResourceName = targetResourceName;
		}

		public Resource getResource() {
			return resource;
		}

		public String getTargetResourceName() {
			return targetResourceName;
		}
	}
	
	
	public static boolean assertActivations(KieSession kieSession, int expected) {
		boolean correctNumberOfActivations = false;
		InternalAgenda agenda = (InternalAgenda) kieSession.getAgenda();
		Activation<?>[] activations = agenda.getActivations();
		if (expected == activations.length) {
			correctNumberOfActivations = true;
		}
		return correctNumberOfActivations;
	}
	
	


}
