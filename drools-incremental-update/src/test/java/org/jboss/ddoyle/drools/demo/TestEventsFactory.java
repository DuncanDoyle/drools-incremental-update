package org.jboss.ddoyle.drools.demo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.jboss.ddoyle.drools.demo.model.v1.SimpleEvent;

public class TestEventsFactory {
	
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmmssSSS");
	
	public static List<SimpleEvent> getFirstSimpleEvents() throws Exception {
		List<SimpleEvent> simpleEvents = new ArrayList<>();
		simpleEvents.add(new SimpleEvent("1", "MY_CODE", DATE_FORMAT.parse("20150223:090000000")));
		simpleEvents.add(new SimpleEvent("2", "MY_CODE", DATE_FORMAT.parse("20150223:090005000")));
		return simpleEvents;
	}

	public static List<SimpleEvent> getSecondSimpleEvents() throws Exception {
		List<SimpleEvent> simpleEvents = new ArrayList<>();
		simpleEvents.add(new SimpleEvent("3", "MY_CODE", DATE_FORMAT.parse("20150223:090021000")));
		return simpleEvents;
	}
	
	

}
