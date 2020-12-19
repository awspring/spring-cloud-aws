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
import io.awspring.cloud.ses.SimpleEmailServiceJavaMailSender;
import io.awspring.cloud.ses.SimpleEmailServiceMailSender;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
 * {@link EnableAutoConfiguration Auto-configuration} for AWS Simple Email Service
 * support.
 *
 * @author Agim Emruli
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(MailSenderAutoConfiguration.class)
@AutoConfigureBefore(SimpleEmailAutoConfiguration.class)
@ConditionalOnClass({ AmazonSimpleEmailService.class, MailSender.class, SimpleEmailServiceJavaMailSender.class })
@ConditionalOnMissingBean(MailSender.class)
@Import(ContextCredentialsAutoConfiguration.class)
@EnableConfigurationProperties(SesProperties.class)
@ConditionalOnProperty(name = "spring.cloud.aws.ses.enabled", havingValue = "true", matchIfMissing = true)
public class SesAutoConfiguration {

	private final AWSCredentialsProvider credentialsProvider;

	private final RegionProvider regionProvider;

	private final ClientConfiguration clientConfiguration;

	public SesAutoConfiguration(ObjectProvider<RegionProvider> regionProvider,
			ObjectProvider<AWSCredentialsProvider> credentialsProvider,
			@Qualifier(GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME) ObjectProvider<ClientConfiguration> globalClientConfiguration,
			@Qualifier("sesClientConfiguration") ObjectProvider<ClientConfiguration> sesClientConfiguration,
			SesProperties properties) {
		this.credentialsProvider = credentialsProvider.getIfAvailable();
		this.regionProvider = properties.getRegion() == null ? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
		this.clientConfiguration = sesClientConfiguration.getIfAvailable(globalClientConfiguration::getIfAvailable);
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonSimpleEmailService.class)
	public AmazonWebserviceClientFactoryBean<AmazonSimpleEmailServiceClient> amazonSimpleEmailService() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonSimpleEmailServiceClient.class, this.credentialsProvider,
				this.regionProvider, this.clientConfiguration);
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
