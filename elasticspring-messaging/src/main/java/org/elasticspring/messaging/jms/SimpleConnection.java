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

import com.amazonaws.services.sqs.AmazonSQS;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

/**
 *
 */
public class SimpleConnection implements Connection {

	private final AmazonSQS amazonSQS;
	private String clientId;

	public SimpleConnection(AmazonSQS amazonSQS) {
		this.amazonSQS = amazonSQS;
	}

	public Session createSession(boolean b, int i) throws JMSException {
		return new SimpleSqsSession(this.amazonSQS);
	}

	public String getClientID() throws JMSException {
		return this.clientId;
	}

	public void setClientID(String s) throws JMSException {
		this.clientId = s;
	}

	public ConnectionMetaData getMetaData() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public ExceptionListener getExceptionListener() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void start() throws JMSException {
	}

	public void stop() throws JMSException {
	}

	public void close() throws JMSException {
	}

	public ConnectionConsumer createConnectionConsumer(Destination destination, String s, ServerSessionPool serverSessionPool, int i) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String s, String s1, ServerSessionPool serverSessionPool, int i) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
