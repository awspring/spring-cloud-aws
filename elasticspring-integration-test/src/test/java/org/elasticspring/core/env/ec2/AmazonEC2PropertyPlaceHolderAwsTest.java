/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.env.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.support.SimpleHttpServerFactoryBean;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("AmazonEC2PropertyPlaceHolderAwsTest-context.xml")
public class AmazonEC2PropertyPlaceHolderAwsTest {

	@Autowired
	private AmazonEC2 amazonEC2Client;

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testGetUserProperties() throws Exception {
		SimpleHttpServerFactoryBean simpleHttpServerFactoryBean = new SimpleHttpServerFactoryBean();

		Map<String, HttpHandler> contexts = new HashMap<String, HttpHandler>();
		contexts.put("/latest/meta-data/instance-id", new InstanceIdHttpHandler("i-2bceb35a"));
		simpleHttpServerFactoryBean.setContexts(contexts);
		simpleHttpServerFactoryBean.afterPropertiesSet();

		AmazonEC2InstanceIdProvider instanceIdProvider = new AmazonEC2InstanceIdProvider("http://localhost:8080/latest/meta-data/instance-id");

		AmazonEC2UserTagPropertySource amazonEC2PropertySource = new AmazonEC2UserTagPropertySource("userTagPropertySource", this.amazonEC2Client);
		amazonEC2PropertySource.setInstanceIdProvider(instanceIdProvider);
		Assert.assertEquals("tagv1", amazonEC2PropertySource.getProperty("tag1").toString());
		Assert.assertEquals("tagv2", amazonEC2PropertySource.getProperty("tag2").toString());
		Assert.assertEquals("tagv3", amazonEC2PropertySource.getProperty("tag3").toString());
		Assert.assertEquals("tagv4", amazonEC2PropertySource.getProperty("tag4").toString());
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
