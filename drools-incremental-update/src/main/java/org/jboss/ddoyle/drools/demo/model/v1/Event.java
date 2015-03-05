package org.jboss.ddoyle.drools.demo.model.v1;

import java.io.Serializable;
import java.util.Date;


public interface Event extends Serializable {
	
	public abstract String getId();
	
	public abstract Date getTimestamp();

}
