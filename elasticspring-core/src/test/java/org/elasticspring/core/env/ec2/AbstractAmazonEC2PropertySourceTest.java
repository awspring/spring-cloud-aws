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

package org.elasticspring.core.env.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class AbstractAmazonEC2PropertySourceTest {

	@Test
	public void testDefaultInstanceIdProvider() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		AbstractAmazonEC2PropertySource abstractAmazonEC2PropertySource = new AbstractAmazonEC2PropertySource("test", amazonEC2) {

			@Override
			protected Map<String, String> createValueMap(String instanceId) {
				return Collections.emptyMap();
			}
		};

		Assert.assertTrue(abstractAmazonEC2PropertySource.getInstanceIdProvider() instanceof AmazonEC2InstanceIdProvider);
	}
}
