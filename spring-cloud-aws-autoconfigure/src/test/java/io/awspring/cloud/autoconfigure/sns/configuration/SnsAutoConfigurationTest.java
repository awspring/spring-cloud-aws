package io.awspring.cloud.autoconfigure.sns.configuration;

import io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration;
import io.awspring.cloud.sns.core.NotificationMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.sns.SnsClient;

import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.utils.AttributeMap;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for class {@link SnsAutoConfiguration}
 *
 * @author Matej Nedic
 */
public class SnsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SnsAutoConfiguration.class));

	@Test
	void snsAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sns.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(SnsClient.class));
	}

	@Test
	void snsAutoConfigurationIsEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sns.enabled:true").run(context -> {
			assertThat(context).hasSingleBean(SnsClient.class);
			assertThat(context).hasSingleBean(NotificationMessagingTemplate.class);

			SnsClient client = context.getBean(SnsClient.class);
			SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(client,
					"clientConfiguration");
			AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration, "attributes");
			assertThat(attributes.get(SdkClientOption.ENDPOINT))
					.isEqualTo(URI.create("https://sns.eu-west-1.amazonaws.com"));
		});
	}

	@Test
	void withCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sns.endpoint:http://localhost:8090").run(context -> {
			SnsClient client = context.getBean(SnsClient.class);
			assertThat(context).hasSingleBean(NotificationMessagingTemplate.class);

			SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(client,
					"clientConfiguration");
			AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration, "attributes");
			assertThat(attributes.get(SdkClientOption.ENDPOINT)).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(attributes.get(SdkClientOption.ENDPOINT_OVERRIDDEN)).isTrue();
		});
	}

}
