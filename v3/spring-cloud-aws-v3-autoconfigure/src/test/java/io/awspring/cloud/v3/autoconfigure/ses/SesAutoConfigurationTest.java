/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.v3.autoconfigure.ses;

import io.awspring.cloud.v3.autoconfigure.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.RegionProviderAutoConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for class {@link SesAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 */
class SesAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SesAutoConfiguration.class));

	@Test
	void mailSenderWithJavaMail() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isSameAs(context.getBean(MailSender.class));
		});
	}

	@Test
	void mailSenderWithSimpleEmail() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("javax.mail.Session")).run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean("simpleMailSender")).isNotNull();
			assertThat(context.getBean("simpleMailSender")).isSameAs(context.getBean(MailSender.class));
		});
	}

	@Test
	void sesAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ses.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(MailSender.class);
			assertThat(context).doesNotHaveBean(JavaMailSender.class);
		});
	}

}
