package io.awspring.cloud.autoconfigure.kinesis;


import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.sns.SnsClientCustomizer;
import io.awspring.cloud.autoconfigure.sns.SnsProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({ KinesisAsyncClient.class})
@EnableConfigurationProperties({ KinesisProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty( value= "spring.cloud.aws.kinesis.enabled", havingValue = "true", matchIfMissing = true)
public class KinesisAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public KinesisAsyncClient kinesisAsyncClient(SnsProperties properties, AwsClientBuilderConfigurer awsClientBuilderConfigurer,
							   ObjectProvider<AwsConnectionDetails> connectionDetails,
							   ObjectProvider<SnsClientCustomizer> snsClientCustomizers,
							   ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers) {
		return awsClientBuilderConfigurer
			.configureSyncClient(KinesisAsyncClient.builder(), properties, connectionDetails.getIfAvailable(),
				snsClientCustomizers.orderedStream(), awsSyncClientCustomizers.orderedStream())
			.build();
	}
}
