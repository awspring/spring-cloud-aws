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

package org.springframework.cloud.aws.context.support.io;

import org.springframework.cloud.aws.IntegrationTestConfig;
import org.springframework.cloud.aws.context.config.annotation.EnableContextResourceLoader;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Agim Emruli
 */
@ContextConfiguration(classes = JavaPathMatchingResourceLoaderAwsTest.JavaPathMatchingResourceLoaderAwsTestConfig.class)
public class JavaPathMatchingResourceLoaderAwsTest
		extends PathMatchingResourceLoaderAwsTest {

	@ContextConfiguration
	@EnableContextResourceLoader
	@Import(IntegrationTestConfig.class)
	static class JavaPathMatchingResourceLoaderAwsTestConfig {

	}

}
