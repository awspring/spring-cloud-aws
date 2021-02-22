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

package io.awspring.cloud.v3.ses.autoconfigure;

import javax.mail.Session;

import io.awspring.cloud.v3.autoconfigure.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.RegionProviderAutoConfiguration;
import io.awspring.cloud.v3.ses.SimpleEmailServiceJavaMailSender;
import io.awspring.cloud.v3.ses.SimpleEmailServiceMailSender;
import io.awspring.cloud.v3.ses.autoconfigure.properties.AwsSesProperties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * {@link EnableAutoConfiguration} for {@link SimpleEmailServiceMailSender} and
 * {@link SimpleEmailServiceJavaMailSender}.
 *
 * @author Arun Patra
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsSesProperties.class)
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.ses.enabled", havingValue = "true", matchIfMissing = true)
public class SesAutoConfiguration {

	private final AwsSesProperties properties;

	public SesAutoConfiguration(AwsSesProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public SesV2Client sesV2Client(AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider) {
		return SesV2Client.builder().credentialsProvider(awsCredentialsProvider).region(awsRegionProvider.getRegion())
				.build();
	}

	@Bean
	@ConditionalOnMissingClass("javax.mail.Session")
	public MailSender simpleMailSender(SesV2Client sesV2Client) {
		return new SimpleEmailServiceMailSender(sesV2Client);
	}

	@Bean
	@ConditionalOnClass(Session.class)
	public JavaMailSender javaMailSender(SesV2Client sesV2Client) {
		return new SimpleEmailServiceJavaMailSender(sesV2Client);
	}

}
