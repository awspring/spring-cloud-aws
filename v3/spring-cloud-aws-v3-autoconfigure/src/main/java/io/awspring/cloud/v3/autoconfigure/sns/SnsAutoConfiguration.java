package io.awspring.cloud.v3.autoconfigure.sns;

import java.util.Optional;

import io.awspring.cloud.v3.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.v3.core.SpringCloudClientConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SNS integration.
 *
 * @author Luis Duarte
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({SnsClient.class, SnsAsyncClient.class})
@EnableConfigurationProperties(SnsProperties.class)
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sns.enabled", havingValue = "true", matchIfMissing = true)
public class SnsAutoConfiguration {

	private SnsProperties properties;

	public SnsAutoConfiguration(final SnsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public SnsClient snsClient(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		Region region = Optional.ofNullable(this.properties.getRegion())
			.filter(reg -> !ObjectUtils.isEmpty(reg))
			.map(Region::of)
			.orElse(regionProvider.getRegion());

		SnsClientBuilder builder = SnsClient.builder()
			.credentialsProvider(credentialsProvider)
			.region(region)
			.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());

		Optional.ofNullable(this.properties.getEndpoint())
			.ifPresent(builder::endpointOverride);

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public SnsAsyncClient snsAsyncClient(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		Region region = Optional.ofNullable(this.properties.getRegion())
			.filter(reg -> !ObjectUtils.isEmpty(reg))
			.map(Region::of)
			.orElse(regionProvider.getRegion());

		SnsAsyncClientBuilder builder = SnsAsyncClient.builder()
			.credentialsProvider(credentialsProvider)
			.region(region)
			.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());

		Optional.ofNullable(this.properties.getEndpoint())
			.ifPresent(builder::endpointOverride);

		return builder.build();
	}
}
