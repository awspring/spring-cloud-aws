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
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 *
 */
public class MessageProducerAdapter implements MessageProducer{

	private final AmazonSQS amazonSQS;
	private final Queue defaultDestination;

	public MessageProducerAdapter(AmazonSQS amazonSQS, Destination defaultDestination) {
		this.amazonSQS = amazonSQS;
		this.defaultDestination = (Queue) defaultDestination;
	}


	public void setDisableMessageID(boolean b) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public boolean getDisableMessageID() throws JMSException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setDisableMessageTimestamp(boolean b) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public boolean getDisableMessageTimestamp() throws JMSException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setDeliveryMode(int i) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public int getDeliveryMode() throws JMSException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setPriority(int i) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public int getPriority() throws JMSException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setTimeToLive(long l) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public long getTimeToLive() throws JMSException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Destination getDestination() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void close() throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void send(Message message) throws JMSException {
		this.send(this.defaultDestination,message);
	}

	public void send(Message message, int i, int i1, long l) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void send(Destination destination, Message message) throws JMSException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
			objectOutputStream.writeObject(message);
			objectOutputStream.flush();
			objectOutputStream.close();
		} catch (IOException e) {
			// TODO: Handle this
		}

		SendMessageRequest sendMessageRequest = new SendMessageRequest(defaultDestination.getQueueName(),new String(outputStream.toByteArray()));
		SendMessageResult result = this.amazonSQS.sendMessage(sendMessageRequest);
		message.setJMSMessageID(result.getMessageId());
	}

	public void send(Destination destination, Message message, int i, int i1, long l) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
