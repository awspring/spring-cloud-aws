/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.context.cache;

import com.amazonaws.util.EC2MetadataUtils;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ElastiCacheAwsTest {

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private CacheManager cacheManager;

	@Test
	public void cacheManagerInitialized() throws Exception {
		String cacheCluster = this.testStackEnvironment.getByLogicalId("CacheCluster");
		Assert.assertNotNull(cacheCluster);

		Cache cache = this.cacheManager.getCache(cacheCluster);
		Assert.assertNotNull(cache);

		if (EC2MetadataUtils.getAvailabilityZone() != null) {
			cache.put("foo","bar");
			String cachedValue = (String) cache.get("foo").get();
			Assert.assertEquals("foo", cachedValue);
		}

	}
}