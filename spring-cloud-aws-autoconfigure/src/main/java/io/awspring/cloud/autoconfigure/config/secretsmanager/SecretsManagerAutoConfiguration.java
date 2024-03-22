package io.awspring.cloud.autoconfigure.config.secretsmanager;

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} for Secrets Manager integration.
 *
 * @author Maciej Walkowiak
 * @since 3.2.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SecretsManagerProperties.class)
@ConditionalOnClass({SecretsManagerClient.class})
@AutoConfigureAfter({CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class})
@ConditionalOnProperty(name = "spring.cloud.aws.secretsmanager.enabled", havingValue = "true", matchIfMissing = true)
public class SecretsManagerAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public SecretsManagerClient secretsManagerClient(SecretsManagerProperties properties,
													 AwsClientBuilderConfigurer awsClientBuilderConfigurer,
													 ObjectProvider<AwsClientCustomizer<SecretsManagerClientBuilder>> customizer,
													 ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer.configure(SecretsManagerClient.builder(), properties, connectionDetails.getIfAvailable(),
			customizer.getIfAvailable()).build();
	}
}
