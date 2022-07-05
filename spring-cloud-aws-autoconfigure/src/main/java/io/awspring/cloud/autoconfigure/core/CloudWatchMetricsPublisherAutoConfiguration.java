package io.awspring.cloud.autoconfigure.core;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.autoconfigure.metrics.CloudWatchProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.time.Duration;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsProperties.class)
@ConditionalOnClass({ CloudWatchMetricPublisher.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class})
public class CloudWatchMetricsPublisherAutoConfiguration {

	private final AwsProperties awsProperties;

	public CloudWatchMetricsPublisherAutoConfiguration(AwsProperties awsProperties) {
		this.awsProperties = awsProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.aws.metrics.enabled", havingValue = "true", matchIfMissing = true)
	MetricPublisher cloudWatchMetricPublisher(AwsClientBuilderConfigurer awsClientBuilderConfigurer, ObjectProvider<AwsClientCustomizer<CloudWatchAsyncClientBuilder>> configurer) {
		PropertyMapper propertyMapper = PropertyMapper.get();

		CloudWatchAsyncClientBuilder cloudWatchAsyncClientBuilder = CloudWatchAsyncClient.builder();
		CloudWatchProperties cloudWatchProperties = new CloudWatchProperties();
		propertyMapper.from(cloudWatchProperties.getEndpoint()).whenNonNull().to(cloudWatchProperties::setEndpoint);
		propertyMapper.from(cloudWatchProperties.getRegion()).whenNonNull().to(cloudWatchProperties::setRegion);
		CloudWatchAsyncClient cloudWatchAsyncClient = awsClientBuilderConfigurer.configure(cloudWatchAsyncClientBuilder, cloudWatchProperties, configurer.getIfAvailable(), null).build();

		CloudWatchMetricPublisher.Builder builder = CloudWatchMetricPublisher.builder();
		builder.cloudWatchClient(cloudWatchAsyncClient);

		if (awsProperties.getMetrics() != null) {
			propertyMapper.from(awsProperties.getMetrics()::getNamespace).whenNonNull().to(builder::namespace);
			propertyMapper.from(awsProperties.getMetrics()::getUploadFrequencyInSeconds).whenNonNull()
				.to(v -> builder.uploadFrequency(Duration.ofSeconds(v)));
		}
		return builder.build();
	}
}
