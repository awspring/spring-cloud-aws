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

package org.springframework.cloud.aws.autoconfigure;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.cloud.aws.support.TestApplication;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Agim Emruli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@IntegrationTest({"cloud.aws.stack.name=IntegrationTestStack",
		"cloud.aws.region.static=EU_WEST_1",
		"cloud.aws.cache.name=CacheCluster",
		"spring.config.location=${els.config.dir}/access.properties"})
public class ContextAutoConfigurationTest {

	@Autowired
	private AWSCredentialsProvider credentialsProvider;

	@Autowired
	private ResourceIdResolver resourceIdResolver;

	@Autowired
	private ListableStackResourceFactory listableStackResourceFactory;

	@Autowired
	private JavaMailSender javaMailSender;

	@Autowired
	private CacheInterceptor cacheInterceptor;

	@Autowired
	private SimpleMessageListenerContainer simpleMessageListenerContainer;

	@Test
	public void credentialsProvider_providerChainConfiguredBecauseCredentialsGiven_returnsAwsCredentialsProvider() throws Exception {
		assertNotNull(this.credentialsProvider);
	}

	@Test
	public void resourceIdResolver_configuredBecauseOfStackConfiguration_resolvesIdToPhysicalId() throws Exception {
		assertNotNull(this.resourceIdResolver.resolveToPhysicalResourceId("EmptyBucket"));
	}

	@Test
	public void listableStackResourceFactory_availableInApplicationContext_returnsAllResources() throws Exception {
		StackResource emptyBucket = null;
		for (StackResource stackResource : this.listableStackResourceFactory.getAllResources()) {
			if ("EmptyBucket".equals(stackResource.getLogicalId())) {
				emptyBucket = stackResource;
				break;
			}
		}
		assertNotNull(emptyBucket);
	}

	@Test
	public void mailSender_configuredBecauseSpringMailSupportAvailable_configuredJavaMailSender() throws Exception {
		assertNotNull(this.javaMailSender);
	}

	@Test
	public void cacheManagerInterceptor_configuredWithExplicitCacheName_configuredCacheInterceptor() throws Exception {
		assertNotNull(this.cacheInterceptor);
	}

	@Test
	public void simpleMessageListenerContainer_withoutExistingContainerBean_configuredAndRunning() throws Exception {
		assertNotNull(this.simpleMessageListenerContainer);
		assertTrue(this.simpleMessageListenerContainer.isRunning());
	}
}