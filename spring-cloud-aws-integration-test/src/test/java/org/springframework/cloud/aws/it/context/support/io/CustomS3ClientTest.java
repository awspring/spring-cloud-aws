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

package org.springframework.cloud.aws.it.context.support.io;

import javax.annotation.PostConstruct;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.context.config.annotation.EnableContextResourceLoader;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that it is possible to overwrite {@link AmazonS3} client used by
 * {@link SimpleStorageResource}. Related issues: -
 * {@see https://github.com/spring-cloud/spring-cloud-aws/issues/640} -
 * {@see https://github.com/spring-cloud/spring-cloud-aws/issues/348}
 */
@SpringBootTest(classes = CustomS3ClientTest.Config.class, properties = { "s3.access=xxx", "s3.secret=yyy" })
public class CustomS3ClientTest {

	@Autowired
	private SomeComponent someComponent;

	@Test
	public void resolvedResourceUsesCustomConfiguredAmazonS3Bean() {
		assertThat(someComponent.resource).isInstanceOf(SimpleStorageResource.class);

		// check if configuration properties provided credentials are really passed to
		// Amazon S3 client used by resource resolver
		SimpleStorageResource s3Resource = (SimpleStorageResource) someComponent.resource;
		AWSStaticCredentialsProvider chain = (AWSStaticCredentialsProvider) ReflectionTestUtils
				.getField(s3Resource.getAmazonS3(), "awsCredentialsProvider");
		assertThat(chain.getCredentials().getAWSSecretKey()).isEqualTo("yyy");
		assertThat(chain.getCredentials().getAWSAccessKeyId()).isEqualTo("xxx");
	}

	@Component
	static class SomeComponent {

		private final ResourceLoader resourceLoader;

		private Resource resource;

		SomeComponent(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		@PostConstruct
		void init() {
			this.resource = resourceLoader.getResource("s3://foo/bar.txt");
		}

	}

	@SpringBootConfiguration
	@EnableContextResourceLoader
	@EnableConfigurationProperties(S3Properties.class)
	@Import(SomeComponent.class)
	static class Config {

		/**
		 * Overwrites default S3 client and uses {@link ConfigurationProperties} annotated
		 * class to build custom client. Makes sure that instance is created after
		 * configuration properties are bound.
		 * @param s3Properties - s3 properties
		 * @return custom Amazon S3 client.
		 */
		@Bean
		@Primary
		public AmazonS3 customAmazonS3(S3Properties s3Properties) {
			AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
					new BasicAWSCredentials(s3Properties.getAccess(), s3Properties.getSecret()));

			return AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider).build();
		}

	}

	@ConfigurationProperties("s3")
	static class S3Properties {

		private String secret;

		private String access;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public String getAccess() {
			return access;
		}

		public void setAccess(String access) {
			this.access = access;
		}

	}

}
