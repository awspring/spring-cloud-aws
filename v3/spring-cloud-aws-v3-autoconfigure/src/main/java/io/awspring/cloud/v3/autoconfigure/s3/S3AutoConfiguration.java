package io.awspring.cloud.v3.autoconfigure.s3;

import io.awspring.cloud.v3.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.v3.core.SpringCloudClientConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({S3Client.class, S3AsyncClient.class})
@EnableConfigurationProperties(S3Properties.class)
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
public class S3AutoConfiguration {
	private S3Properties properties;

	public S3AutoConfiguration(final S3Properties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public S3Client s3Client(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		Region region = Optional.ofNullable(this.properties.getRegion())
			.filter(reg -> !ObjectUtils.isEmpty(reg))
			.map(Region::of)
			.orElse(regionProvider.getRegion());

		S3ClientBuilder builder = S3Client.builder()
			.credentialsProvider(credentialsProvider)
			.region(region)
			.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());

		Optional.ofNullable(this.properties.getEndpoint())
			.ifPresent(builder::endpointOverride);

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public S3AsyncClient snsAsyncClient(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		Region region = Optional.ofNullable(this.properties.getRegion())
			.filter(reg -> !ObjectUtils.isEmpty(reg))
			.map(Region::of)
			.orElse(regionProvider.getRegion());

		S3AsyncClientBuilder builder = S3AsyncClient.builder()
			.credentialsProvider(credentialsProvider)
			.region(region)
			.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());

		Optional.ofNullable(this.properties.getEndpoint())
			.ifPresent(builder::endpointOverride);

		return builder.build();
	}

}
