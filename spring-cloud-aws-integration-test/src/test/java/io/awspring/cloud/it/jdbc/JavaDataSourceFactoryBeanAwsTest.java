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

package org.springframework.cloud.aws.it.jdbc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.it.IntegrationTestConfig;
import org.springframework.cloud.aws.jdbc.config.annotation.EnableRdsInstance;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Agim Emruli
 */
@ContextConfiguration(classes = JavaDataSourceFactoryBeanAwsTest.JavaDataSourceFactoryBeanAwsTestConfig.class)
class JavaDataSourceFactoryBeanAwsTest extends DataSourceFactoryBeanAwsTest {

	@TestConfiguration
	@EnableRdsInstance(dbInstanceIdentifier = "RdsSingleMicroInstance", password = "${rdsPassword}")
	@Import(IntegrationTestConfig.class)
	@ComponentScan
	static class JavaDataSourceFactoryBeanAwsTestConfig {

	}

}
