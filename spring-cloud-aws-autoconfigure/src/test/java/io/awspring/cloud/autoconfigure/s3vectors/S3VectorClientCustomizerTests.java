package io.awspring.cloud.autoconfigure.s3vectors;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link S3VectorClientCustomizer}.
 *
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
public class S3VectorClientCustomizerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
			"spring.cloud.aws.credentials.access-key:noop", "spring.cloud.aws.credentials.secret-key:noop")
		.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
			CredentialsProviderAutoConfiguration.class, S3VectorClientAutoConfiguration.class));

	@Test
	void customClientCustomizer() {
		contextRunner.withUserConfiguration(S3VectorClientCustomizerTests.CustomizerConfig.class).run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(S3VectorsClient.class));
			assertThat(client.getApiCallTimeout()).describedAs("sets property from first customizer")
				.isEqualTo(Duration.ofMillis(2001));
			assertThat(client.getApiCallAttemptTimeout()).describedAs("sets property from second customizer")
				.isEqualTo(Duration.ofMillis(2002));
			assertThat(client.getSyncHttpClient()).describedAs("sets property from common client customizer")
				.isNotNull();
		});
	}

	@Test
	void customClientCustomizerWithOrder() {
		contextRunner.withUserConfiguration(S3VectorClientCustomizerTests.CustomizerConfigWithOrder.class).run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(S3VectorsClient.class));
			assertThat(client.getApiCallTimeout())
				.describedAs("property from the customizer with higher order takes precedence")
				.isEqualTo(Duration.ofMillis(2001));
		});
	}


	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfig {

		@Bean
		S3VectorClientCustomizer customizer() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2001));
				}));
			};
		}

		@Bean
		S3VectorClientCustomizer customizer2() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallAttemptTimeout(Duration.ofMillis(2002));
				}));
			};
		}

		@Bean
		AwsSyncClientCustomizer awsSyncClientCustomizer() {
			return builder -> {
				builder.httpClient(ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build());
			};
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfigWithOrder {

		@Bean
		@Order(2)
		S3VectorClientCustomizer customizer() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2001));
				}));
			};
		}

		@Bean
		@Order(1)
		S3VectorClientCustomizer customizer2() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2000));
				}));
			};
		}
	}
}
