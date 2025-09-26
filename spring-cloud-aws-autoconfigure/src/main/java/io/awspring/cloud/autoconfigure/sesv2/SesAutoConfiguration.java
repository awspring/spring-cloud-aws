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
package io.awspring.cloud.autoconfigure.sesv2;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.*;
import io.awspring.cloud.sesv2.SimpleEmailServiceJavaMailSender;
import io.awspring.cloud.sesv2.SimpleEmailServiceMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

/**
 * {@link EnableAutoConfiguration} for {@link SimpleEmailServiceMailSender} and
 * {@link SimpleEmailServiceJavaMailSender}.
 *
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @author Arun Patra
 */
@AutoConfiguration
@EnableConfigurationProperties(SesProperties.class)
@ConditionalOnClass({ SesV2Client.class, MailSender.class, SimpleEmailServiceJavaMailSender.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sesv2.enabled", havingValue = "true", matchIfMissing = true)
public class SesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SesV2Client sesV2Client(SesProperties properties, AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsClientCustomizer<SesV2ClientBuilder>> configurer,
			ObjectProvider<AwsConnectionDetails> connectionDetails,
			ObjectProvider<SesV2ClientCustomizer> sesClientCustomizers,
			ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers) {
		return awsClientBuilderConfigurer.configureSyncClient(SesV2Client.builder(), properties,
				connectionDetails.getIfAvailable(), configurer.getIfAvailable(), sesClientCustomizers.orderedStream(),
				awsSyncClientCustomizers.orderedStream()).build();
	}

	@Bean
	@ConditionalOnMissingClass("jakarta.mail.Session")
	@ConditionalOnMissingBean
	public MailSender simpleMailSender(SesV2Client sesClient, SesProperties properties) {
		return new SimpleEmailServiceMailSender(sesClient, properties.getIdentityArn(),
				properties.getConfigurationSetName());
	}

	@Bean
	@ConditionalOnClass(name = "jakarta.mail.Session")
	@ConditionalOnMissingBean
	public JavaMailSender javaMailSender(SesV2Client sesClient, SesProperties properties) {
		return new SimpleEmailServiceJavaMailSender(sesClient, properties.getIdentityArn(),
				properties.getConfigurationSetName());
	}

}
