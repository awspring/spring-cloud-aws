/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.ConfiguredAwsPresigner;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import io.awspring.cloud.autoconfigure.s3.provider.MyAesProvider;
import io.awspring.cloud.autoconfigure.s3.provider.MyRsaProvider;
import io.awspring.cloud.s3.InMemoryBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3OutputStream;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.s3accessgrants.plugin.S3AccessGrantsIdentityProvider;
import software.amazon.awssdk.s3accessgrants.plugin.S3AccessGrantsPlugin;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.encryption.s3.S3EncryptionClient;

/**
 * Tests for {@link S3AutoConfiguration}.
 *
 * @author Maciej Walkowiak
 * @author Matej Nedic
 * @author Giacomo Baso
 * @author Ayeshmantha Perera
 */
class S3AutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withClassLoader(new FilteredClassLoader(S3EncryptionClient.class))
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3AutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(S3EncryptionClient.class));

	private final ApplicationContextRunner contextRunnerEncryption = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3AutoConfiguration.class));

	private final ApplicationContextRunner contextRunnerWithoutGrant = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3AutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(S3AccessGrantsPlugin.class, S3EncryptionClient.class));

	@Test
	void setsS3AccessGrantIdentityProvider() {
		contextRunner.run(context -> {
			S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
			ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
			assertThat(client.getIdentityProviders()).isInstanceOf(S3AccessGrantsIdentityProvider.class);
		});
	}

	@Test
	void doesNotSetS3AccessGrantIdentityProvider() {
		contextRunnerWithoutGrant.run(context -> {
			S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
			ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
			assertThat(client.getIdentityProviders()).isNotInstanceOf(S3AccessGrantsIdentityProvider.class);
		});
	}

	@Test
	void createsS3ClientBean() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(S3Client.class);
			assertThat(context).hasSingleBean(S3ClientBuilder.class);
			assertThat(context).hasSingleBean(S3Properties.class);
			assertThat(context).hasSingleBean(S3OutputStreamProvider.class);
		});
	}

	@Test
	void s3AutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.s3.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(S3Client.class);
			assertThat(context).doesNotHaveBean(S3ClientBuilder.class);
			assertThat(context).doesNotHaveBean(S3Properties.class);
		});
	}

	@Test
	void autoconfigurationIsNotTriggeredWhenS3ModuleIsNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(S3OutputStreamProvider.class)).run(context -> {
			assertThat(context).doesNotHaveBean(S3Client.class);
			assertThat(context).doesNotHaveBean(S3ClientBuilder.class);
			assertThat(context).doesNotHaveBean(S3Properties.class);
		});
	}

	@Nested
	class S3ClientTests {
		@Test
		void s3ClientCanBeOverwritten() {
			contextRunnerEncryption
					.withPropertyValues("spring.cloud.aws.s3.encryption.key-id:234abcd-12ab-34cd-56ef-1234567890ab")
					.withUserConfiguration(CustomS3ClientConfiguration.class).run(context -> {
						assertThat(context).hasSingleBean(S3Client.class);
					});
		}

		@Test
		void createsStandardClientWhenCrossRegionAndEncryptionModuleIsNotInClasspath() {
			contextRunnerEncryption.withClassLoader(new FilteredClassLoader(S3EncryptionClient.class)).run(context -> {
				assertThat(context).doesNotHaveBean(S3EncryptionClient.class);
				assertThat(context).hasSingleBean(S3Client.class);
			});
		}

		@Test
		void createsEncryptionClientBackedByRsa() {
			contextRunnerEncryption.withPropertyValues().withUserConfiguration(CustomRsaProvider.class).run(context -> {
				assertThat(context).hasSingleBean(S3EncryptionClient.class);
				assertThat(context).hasSingleBean(S3RsaProvider.class);
			});
		}

		@Test
		void createsEncryptionClientBackedByAes() {
			contextRunnerEncryption.withPropertyValues().withUserConfiguration(CustomAesProvider.class).run(context -> {
				assertThat(context).hasSingleBean(S3EncryptionClient.class);
				assertThat(context).hasSingleBean(S3AesProvider.class);
			});
		}

		@Test
		void createsEncryptionClientBackedByKms() {
			contextRunnerEncryption
					.withPropertyValues("spring.cloud.aws.s3.encryption.key-id:234abcd-12ab-34cd-56ef-1234567890ab")
					.run(context -> {
						assertThat(context).hasSingleBean(S3EncryptionClient.class);
					});
		}
	}

	@Nested
	class OutputStreamProviderTests {

		@Test
		void createsInMemoryBufferingS3OutputStreamProviderWhenBeanDoesNotExistYet() {
			contextRunner
					.run(context -> assertThat(context).hasSingleBean(InMemoryBufferingS3OutputStreamProvider.class));
		}

		@Test
		void customS3OutputStreamProviderCanBeConfigured() {
			contextRunner.withUserConfiguration(CustomS3OutputStreamProviderConfiguration.class)
					.run(context -> assertThat(context).hasSingleBean(CustomS3OutputStreamProvider.class));
		}
	}

	@Nested
	class EndpointConfigurationTests {
		@Test
		void withCustomEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.s3.endpoint:http://localhost:8090").run(context -> {
				S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
				ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
				assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(client.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090").run(context -> {
				S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
				ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
				assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(client.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpointAndS3Endpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
					"spring.cloud.aws.s3.endpoint:http://localhost:9999").run(context -> {
						S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
						ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
						assertThat(client.getIdentityProviders()).isInstanceOf(S3AccessGrantsIdentityProvider.class);
						assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
						assertThat(client.isEndpointOverridden()).isTrue();
					});
		}
	}

	@Nested
	class S3TemplateAutoConfigurationTests {

		@Test
		void withJacksonOnClasspathAutoconfiguresObjectConverter() {
			contextRunner.run(context -> {
				assertThat(context).hasSingleBean(S3ObjectConverter.class);
				assertThat(context).hasSingleBean(S3Template.class);
			});
		}

		@Test
		void withoutJacksonOnClasspathDoesNotConfigureObjectConverter() {
			contextRunner.withClassLoader(new FilteredClassLoader(ObjectMapper.class, S3EncryptionClient.class))
					.run(context -> {
						assertThat(context).doesNotHaveBean(S3ObjectConverter.class);
						assertThat(context).doesNotHaveBean(S3Template.class);
					});
		}

		@Test
		void usesCustomObjectMapperBean() {
			contextRunner.withUserConfiguration(CustomJacksonConfiguration.class).run(context -> {
				S3ObjectConverter s3ObjectConverter = context.getBean(S3ObjectConverter.class);
				assertThat(s3ObjectConverter).extracting("objectMapper")
						.isEqualTo(context.getBean("customObjectMapper"));
			});
		}

		@Test
		void useAwsConfigurerClient() {
			contextRunner.withUserConfiguration(CustomAwsConfigurerClient.class).run(context -> {
				S3ClientBuilder s3ClientBuilder = context.getBean(S3ClientBuilder.class);
				assertThat(s3ClientBuilder.overrideConfiguration().apiCallTimeout()).contains(Duration.ofMillis(1542));
				AttributeMap.Builder attributeMap = resolveAttributeMap(s3ClientBuilder);
				assertThat(attributeMap.get(SdkClientOption.CONFIGURED_SYNC_HTTP_CLIENT)).isNotNull();
			});
		}

		@Test
		void usesCustomS3ObjectConverter() {
			contextRunner
					.withUserConfiguration(CustomJacksonConfiguration.class, CustomS3ObjectConverterConfiguration.class)
					.run(context -> {
						S3ObjectConverter s3ObjectConverter = context.getBean(S3ObjectConverter.class);
						S3ObjectConverter customS3ObjectConverter = (S3ObjectConverter) context
								.getBean("customS3ObjectConverter");
						assertThat(s3ObjectConverter).isEqualTo(customS3ObjectConverter);

						S3Template s3Template = context.getBean(S3Template.class);
						assertThat(s3Template).extracting("s3ObjectConverter").isEqualTo(customS3ObjectConverter);
					});
		}
	}

	@Test
	void setsCommonAwsProperties() {
		contextRunner.withPropertyValues("spring.cloud.aws.dualstack-enabled:true",
				"spring.cloud.aws.fips-enabled:true", "spring.cloud.aws.defaults-mode:MOBILE").run(context -> {
					S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
					ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
					assertThat(client.getDualstackEnabled()).isTrue();
					assertThat(client.getFipsEnabled()).isTrue();
					assertThat(client.getDefaultsMode()).isEqualTo(DefaultsMode.MOBILE);
				});
	}

	@Test
	void setsS3SpecificDualStackProperty() {
		contextRunner.withPropertyValues("spring.cloud.aws.s3.dualstack-enabled:true").run(context -> {
			S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
			ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
			assertThat(client.getDualstackEnabled()).isTrue();
		});
	}

	@Test
	void s3SpecificDualStackOverwritesGlobalDualStack() {
		contextRunner.withPropertyValues("spring.cloud.aws.dualstack-enabled:true",
				"spring.cloud.aws.s3.dualstack-enabled:false").run(context -> {
					S3ClientBuilder builder = context.getBean(S3ClientBuilder.class);
					ConfiguredAwsClient client = new ConfiguredAwsClient(builder.build());
					assertThat(client.getDualstackEnabled()).isFalse();
				});
	}

	@Nested
	class S3PresignerAutoConfigurationTests {

		@Test
		void s3EndpointTakesPriorityOverGlobalEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
					"spring.cloud.aws.s3.endpoint:http://localhost:9999").run(context -> {
						ConfiguredAwsPresigner presigner = new ConfiguredAwsPresigner(
								context.getBean(S3Presigner.class));
						assertThat(presigner.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
						assertThat(presigner.isEndpointOverridden()).isTrue();
					});
		}

		@Test
		void setsCommonAwsPropertiesOnPresigner() {
			contextRunner
					.withPropertyValues("spring.cloud.aws.dualstack-enabled:true", "spring.cloud.aws.fips-enabled:true")
					.run(context -> {
						ConfiguredAwsPresigner presigner = new ConfiguredAwsPresigner(
								context.getBean(S3Presigner.class));
						assertThat(presigner.getDualstackEnabled()).isTrue();
						assertThat(presigner.getFipsEnabled()).isTrue();
						assertThat(presigner.getRegion()).isEqualTo(Region.of("eu-west-1"));
					});
		}

		@Test
		void setsRegionFromProperties() {
			contextRunner.withPropertyValues("spring.cloud.aws.s3.region:us-east-1").run(context -> {
				ConfiguredAwsPresigner presigner = new ConfiguredAwsPresigner(context.getBean(S3Presigner.class));
				assertThat(presigner.getRegion()).isEqualTo(Region.of("us-east-1"));
			});
		}

		@Test
		void setsRegionToDefault() {
			contextRunner.run(context -> {
				ConfiguredAwsPresigner presigner = new ConfiguredAwsPresigner(context.getBean(S3Presigner.class));
				assertThat(presigner.getRegion()).isEqualTo(Region.of("eu-west-1"));
			});
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJacksonConfiguration {
		@Bean
		ObjectMapper customObjectMapper() {
			return new ObjectMapper();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3ObjectConverterConfiguration {

		@Bean
		S3ObjectConverter customS3ObjectConverter() {
			return mock(S3ObjectConverter.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3ClientConfiguration {

		@Bean
		S3Client customS3Client() {
			return mock(S3Client.class);
		}

	}

	@Configuration
	static class CustomRsaProvider {
		@Bean
		S3RsaProvider rsaProvider() {
			return new MyRsaProvider();
		}
	}

	@Configuration
	static class CustomAesProvider {
		@Bean
		S3AesProvider aesProvider() {
			return new MyAesProvider();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAwsConfigurerClient {

		@Bean
		AwsClientCustomizer<S3ClientBuilder> s3ClientBuilderAwsClientConfigurer() {
			return new S3AwsClientClientConfigurer();
		}

		static class S3AwsClientClientConfigurer implements AwsClientCustomizer<S3ClientBuilder> {
			@Override
			@Nullable
			public ClientOverrideConfiguration overrideConfiguration() {
				return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(1542)).build();
			}

			@Override
			@Nullable
			public SdkHttpClient httpClient() {
				return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
			}
		}

	}

	static class CustomS3OutputStreamProvider implements S3OutputStreamProvider {

		@Override
		public S3OutputStream create(String bucket, String key, @Nullable ObjectMetadata metadata) throws IOException {
			return null;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3OutputStreamProviderConfiguration {

		@Bean
		S3OutputStreamProvider customS3OutputStreamProvider() {
			return new CustomS3OutputStreamProvider();
		}

	}

	@Nested
	class AutoConfigurationOrderingTests {

		@Test
		void awsAutoConfigurationCreatesRequiredBeansBeforeS3() {
			new ApplicationContextRunner()
					.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
					.withConfiguration(AutoConfigurations.of(
							CredentialsProviderAutoConfiguration.class,
							RegionProviderAutoConfiguration.class,
							AwsAutoConfiguration.class
					))
					.run(context -> {
						assertThat(context).hasSingleBean(AwsCredentialsProvider.class);
						assertThat(context).hasSingleBean(AwsRegionProvider.class);
						assertThat(context).hasSingleBean(AwsClientBuilderConfigurer.class);
					});
		}

		@Test
		void s3AutoConfigurationUsesAwsClientBuilderConfigurer() {
			contextRunner.run(context -> {
				assertThat(context).hasSingleBean(AwsCredentialsProvider.class);
				assertThat(context).hasSingleBean(AwsRegionProvider.class);
				assertThat(context).hasSingleBean(AwsClientBuilderConfigurer.class);
				assertThat(context).hasSingleBean(S3ClientBuilder.class);
				assertThat(context).hasSingleBean(S3Client.class);

				AwsClientBuilderConfigurer configurer = context.getBean(AwsClientBuilderConfigurer.class);
				S3ClientBuilder s3Builder = context.getBean(S3ClientBuilder.class);
				
				assertThat(configurer).isNotNull();
				assertThat(s3Builder).isNotNull();
			});
		}

		@Test
		void s3ClientBuilderReceivesProperlyConfiguredAwsClientBuilderConfigurer() {
			contextRunner
					.withPropertyValues(
							"spring.cloud.aws.region.static:eu-central-1",
							"spring.cloud.aws.credentials.access-key:config-test-key",
							"spring.cloud.aws.credentials.secret-key:config-test-secret"
					)
					.run(context -> {
						S3Client s3Client = context.getBean(S3Client.class);

						ConfiguredAwsClient client = new ConfiguredAwsClient(s3Client);
						assertThat(client.getRegion()).isEqualTo(Region.of("eu-central-1"));
						assertThat(client.getAwsCredentialsProvider()).isNotNull();
					});
		}

		@Test
		void s3AutoConfigurationFailsGracefullyWithoutAwsAutoConfiguration() {
			new ApplicationContextRunner()
					.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
					.withConfiguration(AutoConfigurations.of(
							CredentialsProviderAutoConfiguration.class,
							RegionProviderAutoConfiguration.class,
							S3AutoConfiguration.class
					))
					.run(context -> {
						assertThat(context).hasFailed();
						assertThat(context.getStartupFailure())
								.hasMessageContaining("AwsClientBuilderConfigurer")
								.isInstanceOf(UnsatisfiedDependencyException.class);
					});
		}
	}

	@Nested
	class CredentialTypesTests {

		@Test
		void s3ClientSupportsWebIdentityTokenCredentials() {
			// Simulate EKS Web Identity Token environment
			contextRunner
					.withSystemProperties(
							"aws.webIdentityTokenFile", "/tmp/test-token",
							"aws.roleArn", "arn:aws:iam::123456789012:role/test-eks-role",
							"aws.roleSessionName", "test-session"
					)
					.withPropertyValues("spring.cloud.aws.region.static:us-east-1")
					.run(context -> {
						assertThat(context).hasSingleBean(AwsCredentialsProvider.class);
						assertThat(context).hasSingleBean(S3Client.class);
						
						AwsCredentialsProvider provider = context.getBean(AwsCredentialsProvider.class);
						assertThat(provider).isNotNull()
								.isInstanceOf(StsWebIdentityTokenFileCredentialsProvider.class);

						S3Client s3Client = context.getBean(S3Client.class);
						assertThat(s3Client).isNotNull();
					});
		}

		@Test
		void s3ClientWithInstanceProfileCredentials() {
			contextRunner
					.withPropertyValues(
							"spring.cloud.aws.region.static:us-east-1",
							"spring.cloud.aws.credentials.instance-profile:true"
					)
					.run(context -> {
						assertThat(context).hasSingleBean(AwsCredentialsProvider.class);
						assertThat(context).hasSingleBean(S3Client.class);
						
						AwsCredentialsProvider provider = context.getBean(AwsCredentialsProvider.class);
						assertThat(provider).isNotNull()
								.isInstanceOf(InstanceProfileCredentialsProvider.class);
						
						S3Client s3Client = context.getBean(S3Client.class);
						assertThat(s3Client).isNotNull();
					});
		}

		@Test
		void s3ClientWithProfileBasedCredentials() {
			contextRunner
					.withPropertyValues(
							"spring.cloud.aws.region.static:us-east-1",
							"spring.cloud.aws.credentials.profile.name:test-profile"
					)
					.run(context -> {
						assertThat(context).hasSingleBean(AwsCredentialsProvider.class);
						assertThat(context).hasSingleBean(S3Client.class);
						
						AwsCredentialsProvider provider = context.getBean(AwsCredentialsProvider.class);
						assertThat(provider).isNotNull()
								.isInstanceOf(ProfileCredentialsProvider.class);
						
						S3Client s3Client = context.getBean(S3Client.class);
						assertThat(s3Client).isNotNull();
					});
		}
	}

	@Nested
	class CustomConfigurationTests {

		@Test
		void customAwsClientBuilderConfigurerIsRespected() {
			contextRunner
					.withUserConfiguration(CustomAwsClientBuilderConfigurerConfiguration.class)
					.run(context -> {
						assertThat(context).hasBean("awsClientBuilderConfigurer");
						assertThat(context).hasBean("customAwsClientBuilderConfigurer");
						assertThat(context).hasSingleBean(S3Client.class);
						
						AwsClientBuilderConfigurer configurer = context.getBean(AwsClientBuilderConfigurer.class);
						assertThat(configurer).isInstanceOf(CustomAwsClientBuilderConfigurerConfiguration.TestAwsClientBuilderConfigurer.class);
					});
		}

		@TestConfiguration
		static class CustomAwsClientBuilderConfigurerConfiguration {
			@Bean
			@Primary
			AwsClientBuilderConfigurer customAwsClientBuilderConfigurer(AwsCredentialsProvider credentialsProvider,
					AwsRegionProvider regionProvider, AwsProperties awsProperties) {
				return new TestAwsClientBuilderConfigurer(credentialsProvider, regionProvider, awsProperties);
			}
			
			static class TestAwsClientBuilderConfigurer extends AwsClientBuilderConfigurer {
				public TestAwsClientBuilderConfigurer(AwsCredentialsProvider credentialsProvider,
						AwsRegionProvider regionProvider, AwsProperties awsProperties) {
					super(credentialsProvider, regionProvider, awsProperties);
				}
			}
		}
	}

	private static AttributeMap.Builder resolveAttributeMap(S3ClientBuilder s3ClientBuilder) {
		AttributeMap.Builder attributes = (AttributeMap.Builder) ReflectionTestUtils
				.getField(ReflectionTestUtils.getField(s3ClientBuilder, "clientConfiguration"), "attributes");
		return Objects.requireNonNull(attributes);
	}
}
