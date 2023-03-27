package io.awspring.cloud.autoconfigure.core;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider;

import java.util.List;

import static io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration.createCredentialsProvider;
import static io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration.createProviderBasedOnListOfProviders;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for STS CredentialsConfiguration integration.
 * @author Eduan Bekker
 * @author Matej Nedic
 */
@AutoConfiguration
@ConditionalOnProperty(name = "spring.cloud.aws.credentials.sts.enabled", havingValue = "true")
@ConditionalOnClass(StsWebIdentityTokenFileCredentialsProvider.class)
@EnableConfigurationProperties(CredentialsProperties.class)
@AutoConfigureBefore(CredentialsProviderAutoConfiguration.class)
@AutoConfigureAfter(RegionProviderAutoConfiguration.class)
public class StsCredentialsProviderAutoConfiguration {

	@Primary
	@Bean
	public AwsCredentialsProvider credentialsProvider(CredentialsProperties properties, AwsRegionProvider regionProvider) {
		List<AwsCredentialsProvider> providers = createCredentialsProvider(properties);
		StsProperties stsProperties = properties.getSts();
		PropertyMapper propertyMapper = PropertyMapper.get();
		StsWebIdentityTokenFileCredentialsProvider.Builder builder = StsWebIdentityTokenFileCredentialsProvider
			.builder().stsClient(StsClient.builder().region(regionProvider.getRegion()).build())
			.asyncCredentialUpdateEnabled(stsProperties.isAsyncCredentialsUpdate());
		propertyMapper.from(stsProperties::getRoleArn).whenNonNull().to(builder::roleArn);
		propertyMapper.from(stsProperties::getWebIdentityTokenFile).whenNonNull().to(builder::webIdentityTokenFile);
		propertyMapper.from(stsProperties::getRoleSessionName).whenNonNull().to(builder::roleSessionName);
		providers.add(builder.build());
		return createProviderBasedOnListOfProviders(providers);
	}
}
