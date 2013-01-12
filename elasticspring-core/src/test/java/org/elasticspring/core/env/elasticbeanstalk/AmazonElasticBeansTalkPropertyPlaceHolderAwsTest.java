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

package org.elasticspring.core.env.elasticbeanstalk;

import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Properties;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class AmazonElasticBeansTalkPropertyPlaceHolderAwsTest {

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testElasticBeans() throws Exception {
		AmazonElasticBeansTalkPropertyPlaceHolder amazonElasticBeansTalkPropertyPlaceHolder = new AmazonElasticBeansTalkPropertyPlaceHolder(
				new AWSElasticBeanstalkClient(new SystemPropertiesCredentialsProvider()));
		amazonElasticBeansTalkPropertyPlaceHolder.resolvePlaceholder("test", new Properties());
		amazonElasticBeansTalkPropertyPlaceHolder.setApplicationName("test");
	}
}
