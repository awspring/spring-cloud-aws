package io.awspring.cloud.v3.autoconfigure.sqs;

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
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

/**
 * {@link EnableAutoConfiguration} for {@link software.amazon.awssdk.services.sqs.SqsClient} and
 * {@link software.amazon.awssdk.services.sqs.SqsAsyncClient}.
 *
 * @author Luis Duarte
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SqsProperties.class)
@ConditionalOnClass({ SqsClient.class, SqsAsyncClient.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SqsAutoConfiguration {

	private SqsProperties properties;

	public SqsAutoConfiguration(final SqsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		Region region = Optional.ofNullable(this.properties.getRegion())
			.filter(reg -> !ObjectUtils.isEmpty(reg))
			.map(Region::of)
			.orElse(regionProvider.getRegion());

		SqsClientBuilder builder = SqsClient.builder()
			.credentialsProvider(credentialsProvider)
			.region(region)
			.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());

		Optional.ofNullable(this.properties.getEndpoint())
			.ifPresent(builder::endpointOverride);

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public SqsAsyncClient sqsAsyncClient(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		Region region = Optional.ofNullable(this.properties.getRegion())
			.filter(reg -> !ObjectUtils.isEmpty(reg))
			.map(Region::of)
			.orElse(regionProvider.getRegion());

		SqsAsyncClientBuilder builder = SqsAsyncClient.builder()
			.credentialsProvider(credentialsProvider)
			.region(region)
			.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());

		Optional.ofNullable(this.properties.getEndpoint())
			.ifPresent(builder::endpointOverride);

		return builder.build();
	}
}
