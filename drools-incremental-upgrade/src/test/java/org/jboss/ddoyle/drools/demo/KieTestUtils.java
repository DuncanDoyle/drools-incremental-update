package org.jboss.ddoyle.drools.demo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.drools.core.time.impl.PseudoClockScheduler;
import org.drools.core.util.FileManager;
import org.jboss.ddoyle.drools.demo.model.v1.Event;
import org.kie.api.KieBase;
import org.kie.api.builder.ReleaseId;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieSession;

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

}
