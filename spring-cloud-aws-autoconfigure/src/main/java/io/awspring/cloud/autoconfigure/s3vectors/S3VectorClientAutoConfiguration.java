package io.awspring.cloud.autoconfigure.s3vectors;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.S3VectorsClientBuilder;

/**
 * @author Matej Nedic
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({S3VectorsClient.class})
@EnableConfigurationProperties({S3VectorProperties.class, AwsProperties.class})
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.s3.vector.enabled", havingValue = "true", matchIfMissing = true)
public class S3VectorClientAutoConfiguration {

	private final S3VectorProperties properties;

	public S3VectorClientAutoConfiguration(S3VectorProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	S3VectorsClientBuilder s3VectorsClientBuilder(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
										   ObjectProvider<AwsClientCustomizer<S3VectorsClientBuilder>> configurer,
										   ObjectProvider<AwsConnectionDetails> connectionDetails,
										   ObjectProvider<S3VectorClientCustomizer> s3ClientCustomizers,
										   ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers) {

        return awsClientBuilderConfigurer.configureSyncClient(S3VectorsClient.builder(), this.properties,
			connectionDetails.getIfAvailable(), configurer.getIfAvailable(), s3ClientCustomizers.orderedStream(),
			awsSyncClientCustomizers.orderedStream());
	}

	@Bean
	S3VectorsClient s3VectorsClient(S3VectorsClientBuilder builder) {
		return builder.build();
	}
}
