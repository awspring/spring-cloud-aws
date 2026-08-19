/*
 * Copyright 2013-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.autoconfigure.kinesis;

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
import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.KinesisProducerConfiguration;

@AutoConfiguration
@ConditionalOnClass({ KinesisProducer.class, KinesisProducerConfiguration.class })
@EnableConfigurationProperties({ KinesisProducerProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(value = "spring.cloud.aws.kinesis.producer.enabled", havingValue = "true", matchIfMissing = true)
public class KinesisProducerAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public KinesisProducerConfiguration kinesisProducerConfiguration(KinesisProducerProperties prop,
			AwsCredentialsProvider credentialsProvider, AwsRegionProvider awsRegionProvider,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		PropertyMapper propertyMapper = PropertyMapper.get();
		KinesisProducerConfiguration config = new KinesisProducerConfiguration();
		propertyMapper.from(prop::getAggregationEnabled).to(config::setAggregationEnabled);
		propertyMapper.from(prop::getAggregationMaxCount).to(config::setAggregationMaxCount);
		propertyMapper.from(prop::getAggregationMaxSize).to(config::setAggregationMaxSize);
		propertyMapper.from(prop::getCloudwatchEndpoint).whenHasText().to(config::setCloudwatchEndpoint);
		propertyMapper.from(prop::getCloudwatchPort).to(config::setCloudwatchPort);
		propertyMapper.from(prop::getCollectionMaxCount).to(config::setCollectionMaxCount);
		propertyMapper.from(prop::getCollectionMaxSize).to(config::setCollectionMaxSize);
		propertyMapper.from(prop::getConnectTimeout).to(config::setConnectTimeout);
		propertyMapper.from(prop::getCredentialsRefreshDelay).to(config::setCredentialsRefreshDelay);
		propertyMapper.from(prop::getEnableCoreDumps).to(config::setEnableCoreDumps);
		propertyMapper.from(prop::getFailIfThrottled).to(config::setFailIfThrottled);
		propertyMapper.from(prop::getLogLevel).whenHasText().to(config::setLogLevel);
		propertyMapper.from(prop::getMaxConnections).to(config::setMaxConnections);
		propertyMapper.from(prop::getMetricsGranularity).whenHasText().to(config::setMetricsGranularity);
		propertyMapper.from(prop::getMetricsLevel).whenHasText().to(config::setMetricsLevel);
		propertyMapper.from(prop::getMetricsNamespace).whenHasText().to(config::setMetricsNamespace);
		propertyMapper.from(prop::getMetricsUploadDelay).to(config::setMetricsUploadDelay);
		propertyMapper.from(prop::getMinConnections).to(config::setMinConnections);
		propertyMapper.from(prop::getNativeExecutable).to(config::setNativeExecutable);
		propertyMapper.from(prop::getRateLimit).to(config::setRateLimit);
		propertyMapper.from(prop::getRecordMaxBufferedTime).to(config::setRecordMaxBufferedTime);
		propertyMapper.from(prop::getRecordTtl).to(config::setRecordTtl);
		propertyMapper.from(prop::getRequestTimeout).to(config::setRequestTimeout);
		propertyMapper.from(prop::getTempDirectory).to(config::setTempDirectory);
		propertyMapper.from(prop::getVerifyCertificate).to(config::setVerifyCertificate);
		propertyMapper.from(prop.getProxyHost()).to(config::setProxyHost);
		propertyMapper.from(prop.getProxyPort()).to(config::setProxyPort);
		propertyMapper.from(prop.getProxyUserName()).whenHasText().to(config::setProxyUserName);
		propertyMapper.from(prop.getProxyPassword()).whenHasText().to(config::setProxyPassword);
		propertyMapper.from(prop.getStsEndpoint()).whenHasText().to(config::setStsEndpoint);
		propertyMapper.from(prop.getStsPort()).to(config::setStsPort);
		propertyMapper.from(prop.getThreadingModel()).to(config::setThreadingModel);
		propertyMapper.from(prop.getThreadPoolSize()).to(config::setThreadPoolSize);
		propertyMapper.from(prop.getUserRecordTimeoutInMillis()).to(config::setUserRecordTimeoutInMillis);

		config.setCredentialsProvider(credentialsProvider);
		config.setRegion(AwsClientBuilderConfigurer
				.resolveRegion(prop, connectionDetails.getIfAvailable(), awsRegionProvider).toString());
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
