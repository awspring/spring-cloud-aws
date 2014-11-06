/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.context;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.support.TestApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Agim Emruli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class,initializers = ContextAutoConfigurationTest.CredentialsApplicationContextInitializer.class)
@IntegrationTest({"cloud.aws.stack.name=IntegrationTestStack","cloud.aws.region.static=EU_WEST_1"})
public class ContextAutoConfigurationTest {

	@Autowired
	private AWSCredentialsProvider credentialsProvider;

	@Autowired
	private ResourceIdResolver resourceIdResolver;

	@Test
	public void credentialsProvider_providerChainConfiguredBecauseCredentialsGiven_returnsAwsCredentialsProvider() throws Exception {
		assertNotNull(this.credentialsProvider);
		assertTrue(AWSCredentialsProviderChain.class.isInstance(this.credentialsProvider));
	}

	@Test
	public void resourceIdResolver_configuredBecauseOfStackConfiguration_resolvesIdToPhysicalId() throws Exception {
		assertNotNull(this.resourceIdResolver.resolveToPhysicalResourceId("EmptyBucket"));
	}

	public static class CredentialsApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>{

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			String accessFile = SystemPropertyUtils.resolvePlaceholders("${els.config.dir}/access.properties");
			try {
				applicationContext.getEnvironment().getPropertySources().addLast(new ResourcePropertySource(
						new FileSystemResource(accessFile)));
			} catch (IOException e) {
				throw new RuntimeException("Error loading access.properties for testing", e);
			}
		}
	}
}