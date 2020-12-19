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

package io.awspring.cloud.autoconfigure.mail;

import java.util.Optional;

import javax.mail.Session;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import io.awspring.cloud.autoconfigure.context.ContextCredentialsAutoConfiguration;
import io.awspring.cloud.context.annotation.ConditionalOnMissingAmazonClient;
import io.awspring.cloud.core.config.AmazonWebserviceClientFactoryBean;
import io.awspring.cloud.core.region.RegionProvider;
import io.awspring.cloud.core.region.StaticRegionProvider;
import io.awspring.cloud.mail.simplemail.SimpleEmailServiceJavaMailSender;
import io.awspring.cloud.mail.simplemail.SimpleEmailServiceMailSender;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;

import static io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;

/**
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @deprecated Use `spring-cloud-starter-aws-ses`
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(MailSenderAutoConfiguration.class)
@ConditionalOnClass({ AmazonSimpleEmailService.class, MailSender.class })
@ConditionalOnMissingBean(MailSender.class)
@Import(ContextCredentialsAutoConfiguration.class)
@EnableConfigurationProperties(SimpleEmailProperties.class)
@ConditionalOnProperty(name = "cloud.aws.mail.enabled", havingValue = "true", matchIfMissing = true)
@Deprecated
public class SimpleEmailAutoConfiguration {

	private final RegionProvider regionProvider;

	private final ClientConfiguration clientConfiguration;

	public SimpleEmailAutoConfiguration(ObjectProvider<RegionProvider> regionProvider,
			@Qualifier(GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME) ObjectProvider<ClientConfiguration> globalClientConfiguration,
			@Qualifier("sesClientConfiguration") ObjectProvider<ClientConfiguration> sesClientConfiguration,
			SimpleEmailProperties properties) {
		this.regionProvider = properties.getRegion() == null ? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
		this.clientConfiguration = sesClientConfiguration.getIfAvailable(globalClientConfiguration::getIfAvailable);
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonSimpleEmailService.class)
	public AmazonWebserviceClientFactoryBean<AmazonSimpleEmailServiceClient> amazonSimpleEmailService(
			AWSCredentialsProvider credentialsProvider, SimpleEmailProperties properties) {
		AmazonWebserviceClientFactoryBean<AmazonSimpleEmailServiceClient> clientFactoryBean = new AmazonWebserviceClientFactoryBean<>(
				AmazonSimpleEmailServiceClient.class, credentialsProvider, this.regionProvider,
				this.clientConfiguration);
		Optional.ofNullable(properties.getEndpoint()).ifPresent(clientFactoryBean::setCustomEndpoint);
		return clientFactoryBean;
	}

	@Bean
	@ConditionalOnMissingClass("javax.mail.Session")
	public MailSender simpleMailSender(AmazonSimpleEmailService amazonSimpleEmailService) {
		return new SimpleEmailServiceMailSender(amazonSimpleEmailService);
	}

	@Bean
	@ConditionalOnClass(Session.class)
	public JavaMailSender javaMailSender(AmazonSimpleEmailService amazonSimpleEmailService) {
		return new SimpleEmailServiceJavaMailSender(amazonSimpleEmailService);
	}

}
