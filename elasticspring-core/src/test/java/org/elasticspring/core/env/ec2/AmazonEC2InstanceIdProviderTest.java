/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.core.env.ec2;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.remoting.support.SimpleHttpServerFactoryBean;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class AmazonEC2InstanceIdProviderTest {


	@Test
	public void testRetrieveInstanceId() throws Exception {
		SimpleHttpServerFactoryBean simpleHttpServerFactoryBean = new SimpleHttpServerFactoryBean();

		Map<String, HttpHandler> contexts = new HashMap<String, HttpHandler>();
		//noinspection HardcodedFileSeparator
		contexts.put("/latest/meta-data/instance-id", new InstanceIdHttpHandler("1234567890"));
		simpleHttpServerFactoryBean.setContexts(contexts);
		simpleHttpServerFactoryBean.afterPropertiesSet();

		try {
			AmazonEC2InstanceIdProvider amazonEC2InstanceIdProvider = new AmazonEC2InstanceIdProvider("http://localhost:8080/latest/meta-data/instance-id");
			String instanceId = amazonEC2InstanceIdProvider.getCurrentInstanceId();
			Assert.assertEquals("1234567890", instanceId);
		} finally {
			simpleHttpServerFactoryBean.destroy();
		}
	}


	public static class InstanceIdHttpHandler implements HttpHandler {

		private final String result;

		public InstanceIdHttpHandler(String result) {
			this.result = result;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			exchange.sendResponseHeaders(200, this.result.length());
			OutputStream outputStream = exchange.getResponseBody();
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
			outputStreamWriter.write(this.result);
			outputStreamWriter.flush();
			exchange.close();
		}
	}
}
