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

package org.springframework.cloud.aws.autoconfigure.mail;

import java.lang.reflect.Field;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleEmailAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SimpleEmailAutoConfiguration.class));

	@Test
	void mailSender_MailSenderWithJava_configuresJavaMailSender() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class))
					.isSameAs(context.getBean(MailSender.class));

			AmazonSimpleEmailServiceClient client = context
					.getBean(AmazonSimpleEmailServiceClient.class);

			Field regionField = ReflectionUtils.findField(client.getClass(),
					"signingRegion");
			ReflectionUtils.makeAccessible(regionField);
			Object regionValue = ReflectionUtils.getField(regionField, client);

			assertThat(regionValue).isEqualTo(Regions.DEFAULT_REGION.getName());
		});
	}

	@Test
	void enableAutoConfigurationWithSpecificRegion() {
		this.contextRunner.withPropertyValues("cloud.aws.mail.region:us-east-1")
				.run(context -> {
					assertThat(context.getBean(MailSender.class)).isNotNull();
					assertThat(context.getBean(JavaMailSender.class)).isNotNull();
					assertThat(context.getBean(JavaMailSender.class))
							.isSameAs(context.getBean(MailSender.class));

					AmazonSimpleEmailServiceClient client = context
							.getBean(AmazonSimpleEmailServiceClient.class);

					Field regionField = ReflectionUtils.findField(client.getClass(),
							"signingRegion");
					ReflectionUtils.makeAccessible(regionField);
					Object regionValue = ReflectionUtils.getField(regionField, client);

					assertThat(regionValue).isEqualTo(Regions.US_EAST_1.getName());
				});
	}

	@Test
	void mailIsDisabled() {
		this.contextRunner.withPropertyValues("cloud.aws.mail.enabled:false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(MailSender.class);
					assertThat(context).doesNotHaveBean(JavaMailSender.class);
				});
	}

}
