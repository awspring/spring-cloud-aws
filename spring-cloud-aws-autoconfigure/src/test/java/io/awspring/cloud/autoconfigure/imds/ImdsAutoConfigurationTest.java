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
package io.awspring.cloud.autoconfigure.imds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.imds.Ec2MetadataClient;

/**
 * Test for the {@link ImdsAutoConfiguration}.
 *
 * @author Ken Krueger
 * @since 3.1.0
 */
public class ImdsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void imdsAutoConfigurationIsDisabled() {
		contextRunner.withPropertyValues("spring.cloud.aws.imds.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(ImdsPropertySource.class));
	}

	@Test
	public void testImdsAutoConfiguration() {
		this.contextRunner.withUserConfiguration(ImdsAutoConfiguration.class).run(context -> {
			context.getBean(ImdsAutoConfiguration.class);
		});
	}

	@Test
	public void testDisableOnMissingDependency() {
		contextRunner.withClassLoader(new FilteredClassLoader(Ec2MetadataClient.class)).run(context -> {
			assertThat(context).doesNotHaveBean(Ec2MetadataClient.class);
			assertThat(context).doesNotHaveBean(ImdsUtils.class);
			assertThat(context).doesNotHaveBean(ImdsPropertySource.class);
			assertThat(context).doesNotHaveBean(ImdsAutoConfiguration.class);
		});
	}

}
