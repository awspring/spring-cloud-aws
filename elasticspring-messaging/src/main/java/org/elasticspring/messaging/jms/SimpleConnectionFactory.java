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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

/**
 *
 */
public class SimpleConnectionFactory implements ConnectionFactory{

	private final AmazonSQS amazonSQS;

	public SimpleConnectionFactory(AmazonSQS amazonSQS) {
		this.amazonSQS = amazonSQS;
	}

	public Connection createConnection() throws JMSException {
		return new SimpleConnection(this.amazonSQS);
	}

	public Connection createConnection(String s, String s1) throws JMSException {
		return new SimpleConnection(new AmazonSQSClient(new BasicAWSCredentials(s,s1)));
	}
}
