/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.support.profile.AmazonWebserviceProfileValueSource;
import org.springframework.cloud.aws.support.profile.IfAmazonWebserviceEnvironment;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ProfileValueSourceConfiguration(AmazonWebserviceProfileValueSource.class)
public abstract class ElastiCacheAwsTest {

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private CachingService cachingService;

	@Before
	public void resetInvocationCount() throws Exception {
		this.cachingService.resetInvocationCount();
	}

	@Test
	@IfAmazonWebserviceEnvironment
	public void expensiveServiceWithCacheManager() throws Exception {
		this.cachingService.deleteCacheKey("foo");
		this.cachingService.deleteCacheKey("bar");

		assertEquals(0, this.cachingService.getInvocationCount().get());

		assertEquals("FOO", this.cachingService.expensiveMethod("foo"));
		assertEquals(1, this.cachingService.getInvocationCount().get());

		assertEquals("FOO", this.cachingService.expensiveMethod("foo"));
		assertEquals(1, this.cachingService.getInvocationCount().get());

		assertEquals("BAR", this.cachingService.expensiveMethod("bar"));
		assertEquals(2, this.cachingService.getInvocationCount().get());
	}

	@Test
	@IfAmazonWebserviceEnvironment
	public void expensiveServiceWithRedisCacheManager() throws Exception {
		this.cachingService.deleteRedisCacheKey("foo");
		this.cachingService.deleteRedisCacheKey("bar");

		assertEquals(0, this.cachingService.getInvocationCount().get());

		assertEquals("FOO", this.cachingService.expensiveRedisMethod("foo"));
		assertEquals(1, this.cachingService.getInvocationCount().get());

		assertEquals("FOO", this.cachingService.expensiveRedisMethod("foo"));
		assertEquals(1, this.cachingService.getInvocationCount().get());

		assertEquals("BAR", this.cachingService.expensiveRedisMethod("bar"));
		assertEquals(2, this.cachingService.getInvocationCount().get());
	}

}
