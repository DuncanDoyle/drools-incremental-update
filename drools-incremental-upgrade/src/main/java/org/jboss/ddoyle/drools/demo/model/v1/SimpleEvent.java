package org.jboss.ddoyle.drools.demo.model.v1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SimpleEvent implements Event {
	
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmmssSSS");
	
	/**
	 * SerialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	private final String id;

	//private final Date timestamp;
	
	private final long timestamp;
	
	private final String code; 
	
	public SimpleEvent(final String code, final Date eventTimestamp) {
		this(UUID.randomUUID().toString(), code, eventTimestamp);
	}
	
	public SimpleEvent(final String eventId, final String code, final Date eventTimestamp) {
		this.id = eventId;
		this.code = code;
		this.timestamp = eventTimestamp.getTime();
	}
	
	public String getId() {
		return id;
	}
	
	public String getCode() {
		return code;
	}

	public Date getTimestamp() {
		return new Date(timestamp);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", id).append("timestamp", DATE_FORMAT.format(timestamp)).toString();
	}
	

}
