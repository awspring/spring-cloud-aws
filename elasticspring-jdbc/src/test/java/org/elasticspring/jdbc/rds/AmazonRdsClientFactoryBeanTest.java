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

package org.elasticspring.jdbc.rds;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.rds.AmazonRDS;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test class for {@link AmazonRdsClientFactoryBean}
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsClientFactoryBeanTest {


	@Test
	public void testCreateAndDestroyInstance() throws Exception {
		AmazonRdsClientFactoryBean amazonRdsClientFactoryBean = new AmazonRdsClientFactoryBean(new StaticCredentialsProvider(new AnonymousAWSCredentials()));
		amazonRdsClientFactoryBean.afterPropertiesSet();
		AmazonRDS amazonRDS = amazonRdsClientFactoryBean.getObject();
		Assert.assertNotNull(amazonRDS);
		amazonRdsClientFactoryBean.destroy();
	}
}
