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

package org.springframework.cloud.aws.autoconfigure.mail;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.mail.simplemail.SimpleEmailServiceJavaMailSender;
import org.springframework.cloud.aws.mail.simplemail.SimpleEmailServiceMailSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * @author Agim Emruli
 */
@Configuration
@AutoConfigureAfter(org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration.class)
@ConditionalOnClass(name = { "org.springframework.mail.MailSender",
		"com.amazonaws.services.simpleemail.AmazonSimpleEmailService" })
@ConditionalOnMissingBean(MailSender.class)
@Import(ContextCredentialsAutoConfiguration.class)
public class MailSenderAutoConfiguration {

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonSimpleEmailService.class)
	public AmazonWebserviceClientFactoryBean<AmazonSimpleEmailServiceClient> amazonSimpleEmailService(
			AWSCredentialsProvider credentialsProvider) {
		return new AmazonWebserviceClientFactoryBean<>(
				AmazonSimpleEmailServiceClient.class, credentialsProvider,
				this.regionProvider);
	}

	@Bean
	@ConditionalOnMissingClass("org.springframework.cloud.aws.mail.simplemail.SimpleEmailServiceJavaMailSender")
	public MailSender simpleMailSender(
			AmazonSimpleEmailService amazonSimpleEmailService) {
		return new SimpleEmailServiceMailSender(amazonSimpleEmailService);
	}

	@Bean
	@ConditionalOnClass(name = "javax.mail.Session")
	public JavaMailSender javaMailSender(
			AmazonSimpleEmailService amazonSimpleEmailService) {
		return new SimpleEmailServiceJavaMailSender(amazonSimpleEmailService);
	}

}
