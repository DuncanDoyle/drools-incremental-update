package org.jboss.ddoyle.drools.demo.listener;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

public class RulesFiredAgendaEventListener extends DefaultAgendaEventListener {

	private Map<String, Integer> rulesFired = new HashMap<>();
	
	@Override
	public void afterMatchFired(AfterMatchFiredEvent event) {
		//Just for testing purposes. Better would be to use AtomicLong and ConcurrentHashMap.	
		synchronized(this) {
			String ruleName = event.getMatch().getRule().getPackageName() + "-" + event.getMatch().getRule().getName();
			Integer nrOfFires = rulesFired.get(ruleName);
			if (nrOfFires == null) {
				nrOfFires = 0;
			}
			nrOfFires = nrOfFires + 1;
			rulesFired.put(ruleName, nrOfFires);
		}
	}
	
	
	public int getNrOfRulesFired(String ruleName) {
		Integer nrOfRules = rulesFired.get(ruleName);
		if (nrOfRules == null) {
			return 0;
		}
		return nrOfRules;
	}

}
