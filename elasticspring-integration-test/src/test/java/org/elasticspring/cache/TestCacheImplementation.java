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

package org.elasticspring.cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@ContextConfiguration("TestCacheImplementation-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class TestCacheImplementation {

	@Autowired
	private MyService myService;

	@Autowired
	private CacheManager cacheManager;

	@Test
	public void testCaching() throws Exception {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		this.myService.longComputation(12);
		stopWatch.stop();
		stopWatch.start();
		this.myService.longComputation(12);
		stopWatch.stop();
		System.out.println(stopWatch.prettyPrint());
	}



	public static class MyService {

		@Cacheable(value = "defaultCache", key = "#nb")
		public int longComputation(int nb) throws InterruptedException {
			Thread.sleep(2000L);
			return nb * 10;
		}

	}

}
