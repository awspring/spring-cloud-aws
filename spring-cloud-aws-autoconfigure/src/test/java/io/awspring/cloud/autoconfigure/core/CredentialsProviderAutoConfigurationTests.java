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
package io.awspring.cloud.autoconfigure.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider;

class CredentialsProviderAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CredentialsProviderAutoConfiguration.class,
					RegionProviderAutoConfiguration.class))
			.withPropertyValues("spring.cloud.aws.region.static=eu-west-1");

	@TempDir
	static Path tokenTempDir;

	// @checkstyle:off
	@Test
	void credentialsProvider_noExplicitCredentialsProviderConfigured_configuresDefaultAwsCredentialsProviderChain() {
		// @checkstyle:on
		this.contextRunner.run((context) -> {
			AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
					AwsCredentialsProvider.class);
			assertThat(awsCredentialsProvider).isNotNull().isInstanceOf(DefaultCredentialsProvider.class);
		});

	}

	// @checkstyle:off
	@Test
	void credentialsProvider_accessKeyAndSecretKeyConfigured_configuresStaticCredentialsProviderWithAccessAndSecretKey() {
		// @checkstyle:on
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.accessKey:foo",
				"spring.cloud.aws.credentials.secretKey:bar").run((context) -> {
					AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
							StaticCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();
					assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("foo");
					assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("bar");
				});
	}

	@Test
	void credentialsProvider_instanceProfileConfigured_configuresInstanceProfileCredentialsProvider() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.instance-profile:true").run((context) -> {
			AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
					InstanceProfileCredentialsProvider.class);
			assertThat(awsCredentialsProvider).isNotNull();
		});
	}

	@Test
	void credentialsProvider_profileNameAndPathConfigured_configuresProfileCredentialsProvider() throws IOException {
		this.contextRunner.withPropertyValues("spring.cloud.aws.credentials.profile.name:customProfile",
				"spring.cloud.aws.credentials.profile.path:"
						+ new ClassPathResource(getClass().getSimpleName() + "-profile", getClass()).getFile()
								.getAbsolutePath())
				.run((context) -> {
					AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
							ProfileCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull();
					assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("testAccessKey");
					assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey())
							.isEqualTo("testSecretKey");
				});
	}

	@Test
	void credentialsProvider_stsPropertiesConfigured_configuresStsWebIdentityTokenFileCredentialsProvider()
			throws IOException {
		File tempFile = tokenTempDir.resolve("token-file.txt").toFile();
		tempFile.createNewFile();

		this.contextRunner
				.withPropertyValues("spring.cloud.aws.region.static:af-south-1",
						"spring.cloud.aws.credentials.sts.role-arn:develop",
						"spring.cloud.aws.credentials.sts.web-identity-token-file:" + tempFile.getAbsolutePath())
				.run((context) -> {
					AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
							AwsCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull()
							.isInstanceOf(StsWebIdentityTokenFileCredentialsProvider.class);
				});
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void credentialsProvider_stsCredentialsProviderNotConfigured_whenWebIdentityTokenNotConfigured(CapturedOutput output)
		throws IOException {
		this.contextRunner
			.withPropertyValues("spring.cloud.aws.region.static:af-south-1")
			.run((context) -> {
				AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
					AwsCredentialsProvider.class);
				assertThat(awsCredentialsProvider).isNotNull()
					.isInstanceOf(DefaultCredentialsProvider.class);
			});
		assertThat(output).doesNotContain("Skipping creating `StsCredentialsProvider`");
	}

	@Test
	void credentialsProvider_stsSystemPropertiesDefault_configuresStsWebIdentityTokenFileCredentialsProvider()
			throws IOException {
		File tempFile = tokenTempDir.resolve("token-file.txt").toFile();
		tempFile.createNewFile();

		this.contextRunner.withPropertyValues("spring.cloud.aws.region.static:af-south-1")
				.withSystemProperties("aws.roleArn=develop", "aws.webIdentityTokenFile=" + tempFile.getAbsolutePath())
				.run((context) -> {
					AwsCredentialsProvider awsCredentialsProvider = context.getBean("credentialsProvider",
							AwsCredentialsProvider.class);
					assertThat(awsCredentialsProvider).isNotNull()
							.isInstanceOf(StsWebIdentityTokenFileCredentialsProvider.class);
				});
	}

	@Test
	void credentialsProvider_customCredentialsConfigured_customCredentialsAreUsed() {
		// @checkstyle:on
		this.contextRunner.withUserConfiguration(CustomCredentialsProviderConfiguration.class).run((context) -> {
			AwsCredentialsProvider awsCredentialsProvider = context.getBean(AwsCredentialsProvider.class);
			assertThat(awsCredentialsProvider).isNotNull().isInstanceOf(CustomAWSCredentialsProvider.class);
		});
	}

	@Test
	void isNotCreatedWhenAwsAuthModuleIsNotInClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(AwsCredentialsProvider.class)).run(context -> {
			assertThat(context).doesNotHaveBean(CredentialsProviderAutoConfiguration.class);
		});
	}

	@Test
	void isNotCreatedWhenAwsProfilesModuleIsNotInClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ProfileFile.class)).run(context -> {
			assertThat(context).doesNotHaveBean(CredentialsProviderAutoConfiguration.class);
		});
	}

	@Configuration
	static class CustomCredentialsProviderConfiguration {

		@Bean
		public AwsCredentialsProvider customAwsCredentialsProvider() {
			return new CustomAWSCredentialsProvider();
		}

	}

	static class CustomAWSCredentialsProvider implements AwsCredentialsProvider {

		@Override
		public AwsCredentials resolveCredentials() {
			return null;
		}

	}

}
