/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 *
 */
public class JmsMessageAdapter implements Message, Serializable {

	private String messageId;
	private long timeStamp;
	private String correlationId;
	private Destination replyTo;
	private Destination destination;
	private int deliveryMode;
	private boolean reDelivered;
	private String type;
	private long expiration;
	private int priority;

	private final Map<String, Object> customProperties = new HashMap<String, Object>();

	public String getJMSMessageID() throws JMSException {
		return this.messageId;
	}

	public void setJMSMessageID(String messageId) throws JMSException {
		this.messageId = messageId;
	}

	public long getJMSTimestamp() throws JMSException {
		return this.timeStamp;
	}

	public void setJMSTimestamp(long timeStamp) throws JMSException {
		this.timeStamp = timeStamp;
	}

	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		return correlationId.getBytes();
	}

	public void setJMSCorrelationIDAsBytes(byte[] correlationId) throws JMSException {
		this.correlationId = new String(correlationId);
	}

	public void setJMSCorrelationID(String correlationId) throws JMSException {
		this.correlationId = correlationId;
	}

	public String getJMSCorrelationID() throws JMSException {
		return this.correlationId;
	}

	public Destination getJMSReplyTo() throws JMSException {
		return this.replyTo;
	}

	public void setJMSReplyTo(Destination destination) throws JMSException {
		this.replyTo = destination;
	}

	public Destination getJMSDestination() throws JMSException {
		return this.destination;
	}

	public void setJMSDestination(Destination destination) throws JMSException {
		this.destination = destination;
	}

	public int getJMSDeliveryMode() throws JMSException {
		return this.deliveryMode;
	}

	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
		this.deliveryMode = deliveryMode;
	}

	public boolean getJMSRedelivered() throws JMSException {
		return this.reDelivered;
	}

	public void setJMSRedelivered(boolean b) throws JMSException {
		this.reDelivered = b;
	}

	public String getJMSType() throws JMSException {
		return this.type;
	}

	public void setJMSType(String s) throws JMSException {
		this.type = s;
	}

	public long getJMSExpiration() throws JMSException {
		return this.expiration;
	}

	public void setJMSExpiration(long l) throws JMSException {
		this.expiration = l;
	}

	public int getJMSPriority() throws JMSException {
		return this.priority;
	}

	public void setJMSPriority(int i) throws JMSException {
		this.priority = i;
	}

	public void clearProperties() throws JMSException {
		this.customProperties.clear();
	}

	public boolean propertyExists(String s) throws JMSException {
		return this.customProperties.containsKey(s);
	}

	public boolean getBooleanProperty(String s) throws JMSException {
		return (Boolean) this.customProperties.get(s);
	}

	public byte getByteProperty(String s) throws JMSException {
		return (Byte) this.customProperties.get(s);
	}

	public short getShortProperty(String s) throws JMSException {
		return (Short) this.customProperties.get(s);
	}

	public int getIntProperty(String s) throws JMSException {
		return (Integer) this.customProperties.get(s);
	}

	public long getLongProperty(String s) throws JMSException {
		return (Long) this.customProperties.get(s);
	}

	public float getFloatProperty(String s) throws JMSException {
		return (Float) this.customProperties.get(s);
	}

	public double getDoubleProperty(String s) throws JMSException {
		return (Double) this.customProperties.get(s);
	}

	public String getStringProperty(String s) throws JMSException {
		return (String) this.customProperties.get(s);
	}

	public Object getObjectProperty(String s) throws JMSException {
		return this.customProperties.get(s);
	}

	public Enumeration getPropertyNames() throws JMSException {
		//noinspection UseOfObsoleteCollectionType
		return new Hashtable<String,Object>(this.customProperties).keys();
	}

	public void setBooleanProperty(String s, boolean b) throws JMSException {
		this.customProperties.put(s,b);
	}

	public void setByteProperty(String s, byte b) throws JMSException {
		this.customProperties.put(s,b);
	}

	public void setShortProperty(String s, short i) throws JMSException {
		this.customProperties.put(s,i);
	}

	public void setIntProperty(String s, int i) throws JMSException {
		this.customProperties.put(s,i);
	}

	public void setLongProperty(String s, long l) throws JMSException {
		this.customProperties.put(s,l);
	}

	public void setFloatProperty(String s, float v) throws JMSException {
		this.customProperties.put(s,v);
	}

	public void setDoubleProperty(String s, double v) throws JMSException {
		this.customProperties.put(s,v);
	}

	public void setStringProperty(String s, String s1) throws JMSException {
		this.customProperties.put(s,s1);
	}

	public void setObjectProperty(String s, Object o) throws JMSException {
		this.customProperties.put(s,o);
	}

	public void acknowledge() throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void clearBody() throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
