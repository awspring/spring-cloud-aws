/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.ses;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Tests for class {@link SesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 */
class SesAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SesAutoConfiguration.class));

	@Test
	void mailSenderWithJavaMail() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MailSender.class);
			assertThat(context).hasSingleBean(JavaMailSender.class);
			assertThat(context).getBean(JavaMailSender.class).isSameAs(context.getBean(MailSender.class));
		});
	}

	@Test
	void mailSenderWithoutSesClientInTheClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(software.amazon.awssdk.services.ses.SesClient.class))
				.run(context -> {
					assertThat(context).doesNotHaveBean(MailSender.class);
					assertThat(context).doesNotHaveBean(JavaMailSender.class);
				});
	}

	@Test
	void mailSenderWithSimpleEmail() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(jakarta.mail.Session.class)).run(context -> {
			assertThat(context).hasSingleBean(MailSender.class);
			assertThat(context).getBean("simpleMailSender").isNotNull().isSameAs(context.getBean(MailSender.class));
		});
	}

	@Test
	void sesAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ses.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(MailSender.class);
			assertThat(context).doesNotHaveBean(JavaMailSender.class);
		});
	}

	@Test
	void withCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ses.endpoint:http://localhost:8090").run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SesClient.class));
			assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(client.isEndpointOverridden()).isTrue();
		});
	}

	@Test
	void withCustomGlobalEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090").run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SesClient.class));
			assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(client.isEndpointOverridden()).isTrue();
		});
	}

	@Test
	void withCustomGlobalEndpointAndSesEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
				"spring.cloud.aws.ses.endpoint:http://localhost:9999").run(context -> {
					ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SesClient.class));
					assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
					assertThat(client.isEndpointOverridden()).isTrue();
				});
	}

}
