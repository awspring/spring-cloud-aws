package io.awspring.cloud.autoconfigure.kinesis;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class KinesisClientCustomizerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
			"spring.cloud.aws.credentials.access-key:noop", "spring.cloud.aws.credentials.secret-key:noop")
		.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
			CredentialsProviderAutoConfiguration.class, KinesisAutoConfiguration.class));

	@Test
	void customKinesisClientCustomizer() {
		contextRunner.withUserConfiguration(KinesisClientCustomizerTests.CustomizerConfig.class).run(context -> {
			ConfiguredAwsClient kinesisAsyncClient = new ConfiguredAwsClient(context.getBean(KinesisAsyncClient.class));
			assertThat(kinesisAsyncClient.getApiCallTimeout()).describedAs("sets property from first customizer")
				.isEqualTo(Duration.ofMillis(2001));
			assertThat(kinesisAsyncClient.getApiCallAttemptTimeout()).describedAs("sets property from second customizer")
				.isEqualTo(Duration.ofMillis(2002));
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfig {

		@Bean
		KinesisAsyncClientCustomizer kinesisClientCustomizer() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2001));
				}));
			};
		}

		@Bean
		KinesisAsyncClientCustomizer kinesisClientCustomizer2() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallAttemptTimeout(Duration.ofMillis(2002));
				}));
			};
		}
	}
}
