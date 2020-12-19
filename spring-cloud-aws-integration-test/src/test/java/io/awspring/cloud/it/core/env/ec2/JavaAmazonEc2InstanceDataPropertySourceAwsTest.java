/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.it.core.env.ec2;

import org.springframework.cloud.aws.context.config.annotation.EnableContextInstanceData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Agim Emruli
 */
@ContextConfiguration(
		classes = JavaAmazonEc2InstanceDataPropertySourceAwsTest.JavaAmazonEc2InstanceDataPropertySourceAwsTestConfig.class)
class JavaAmazonEc2InstanceDataPropertySourceAwsTest extends AmazonEc2InstanceDataPropertySourceAwsTest {

	@Configuration
	@EnableContextInstanceData
	static class JavaAmazonEc2InstanceDataPropertySourceAwsTestConfig {

		@Bean
		static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		public SimpleConfigurationBean configurationBean() {
			return new SimpleConfigurationBean();
		}

	}

}
