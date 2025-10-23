package io.awspring.cloud.autoconfigure.kinesis;


import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.KinesisProducerConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

@AutoConfiguration
@ConditionalOnClass({ KinesisProducer.class, KinesisProducerConfiguration.class })
@EnableConfigurationProperties({ KinesisProducerProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(value = "spring.cloud.aws.kinesis.producer.enabled", havingValue = "true", matchIfMissing = true)
public class KinesisProducerAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public KinesisProducerConfiguration kinesisProducerConfiguration(KinesisProducerProperties prop,
																	 AwsCredentialsProvider credentialsProvider,
																	 AwsRegionProvider awsRegionProvider, ObjectProvider<AwsConnectionDetails> connectionDetails) {
		PropertyMapper propertyMapper = PropertyMapper.get();
		KinesisProducerConfiguration config = new KinesisProducerConfiguration();
		propertyMapper.from(prop::getAggregationEnabled).whenNonNull().to(config::setAggregationEnabled);
		propertyMapper.from(prop::getAggregationMaxCount).whenNonNull().to(config::setAggregationMaxCount);
		propertyMapper.from(prop::getAggregationMaxSize).whenNonNull().to(config::setAggregationMaxSize);
		propertyMapper.from(prop::getCloudwatchEndpoint).whenHasText().to(config::setCloudwatchEndpoint);
		propertyMapper.from(prop::getCloudwatchPort).whenNonNull().to(config::setCloudwatchPort);
		propertyMapper.from(prop::getCollectionMaxCount).whenNonNull().to(config::setCollectionMaxCount);
		propertyMapper.from(prop::getCollectionMaxSize).whenNonNull().to(config::setCollectionMaxSize);
		propertyMapper.from(prop::getConnectTimeout).whenNonNull().to(config::setConnectTimeout);
		propertyMapper.from(prop::getCredentialsRefreshDelay).whenNonNull().to(config::setCredentialsRefreshDelay);
		propertyMapper.from(prop::getEnableCoreDumps).whenNonNull().to(config::setEnableCoreDumps);
		propertyMapper.from(prop::getFailIfThrottled).whenNonNull().to(config::setFailIfThrottled);
		propertyMapper.from(prop::getLogLevel).whenHasText().to(config::setLogLevel);
		propertyMapper.from(prop::getMaxConnections).whenNonNull().to(config::setMaxConnections);
		propertyMapper.from(prop::getMetricsGranularity).whenHasText().to(config::setMetricsGranularity);
		propertyMapper.from(prop::getMetricsLevel).whenHasText().to(config::setMetricsLevel);
		propertyMapper.from(prop::getMetricsNamespace).whenHasText().to(config::setMetricsNamespace);
		propertyMapper.from(prop::getMetricsUploadDelay).whenNonNull().to(config::setMetricsUploadDelay);
		propertyMapper.from(prop::getMinConnections).whenNonNull().to(config::setMinConnections);
		propertyMapper.from(prop::getNativeExecutable).whenNonNull().to(config::setNativeExecutable);
		propertyMapper.from(prop::getRateLimit).whenNonNull().to(config::setRateLimit);
		propertyMapper.from(prop::getRecordMaxBufferedTime).whenNonNull().to(config::setRecordMaxBufferedTime);
		propertyMapper.from(prop::getRecordTtl).whenNonNull().to(config::setRecordTtl);
		propertyMapper.from(prop::getRequestTimeout).whenNonNull().to(config::setRequestTimeout);
		propertyMapper.from(prop::getTempDirectory).whenNonNull().to(config::setTempDirectory);
		propertyMapper.from(prop::getVerifyCertificate).whenNonNull().to(config::setVerifyCertificate);
		propertyMapper.from(prop.getProxyHost()).whenNonNull().to(config::setProxyHost);
		propertyMapper.from(prop.getProxyPort()).whenNonNull().to(config::setProxyPort);
		propertyMapper.from(prop.getProxyUserName()).whenHasText().to(config::setProxyUserName);
		propertyMapper.from(prop.getProxyPassword()).whenHasText().to(config::setProxyPassword);
		propertyMapper.from(prop.getStsEndpoint()).whenHasText().to(config::setStsEndpoint);
		propertyMapper.from(prop.getStsPort()).whenNonNull().to(config::setStsPort);
		propertyMapper.from(prop.getThreadingModel()).whenNonNull().to(config::setThreadingModel);
		propertyMapper.from(prop.getThreadPoolSize()).whenNonNull().to(config::setThreadPoolSize);
		propertyMapper.from(prop.getUserRecordTimeoutInMillis()).whenNonNull()
			.to(config::setUserRecordTimeoutInMillis);

		config.setCredentialsProvider(credentialsProvider);
		config.setRegion(AwsClientBuilderConfigurer
			.resolveRegion(prop, connectionDetails.getIfAvailable(), awsRegionProvider)
			.toString());
		connectionDetails.ifAvailable(cd -> {
			config.setKinesisPort(cd.getEndpoint().getPort());
			config.setKinesisEndpoint(cd.getEndpoint().getHost());
		});
		return config;
	}

	@ConditionalOnMissingBean
	@Bean
	public KinesisProducer kinesisProducer(KinesisProducerConfiguration kinesisProducerConfiguration) {
		return new KinesisProducer(kinesisProducerConfiguration);
	}
}
