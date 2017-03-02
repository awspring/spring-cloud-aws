/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.autoconfigure.actuate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.actuate.metrics.BufferingCloudWatchMetricSender;
import org.springframework.cloud.aws.actuate.metrics.CloudWatchMetricSender;
import org.springframework.cloud.aws.actuate.metrics.CloudWatchMetricWriter;
import org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Autoconfiguration which creates and exports a {@link CloudWatchMetricWriter} alongside
 * with an {@link AmazonCloudWatchAsync} if its not already present.
 *
 * @author Simon Buettner
 * @author Agim Emruli
 */
@Configuration
@Import(ContextCredentialsAutoConfiguration.class)
@EnableConfigurationProperties(CloudWatchMetricProperties.class)
@ConditionalOnProperty(prefix = "cloud.aws.cloudwatch", name = "namespace")
@ConditionalOnClass(name = {"com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync",
		"org.springframework.cloud.aws.actuate.metrics.CloudWatchMetricWriter"})
public class CloudWatchMetricAutoConfiguration {

    @Autowired(required = false)
    private RegionProvider regionProvider;

    @Autowired
	private CloudWatchMetricProperties cloudWatchMetricProperties;

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonCloudWatchAsync.class)
	public AmazonWebserviceClientFactoryBean<AmazonCloudWatchAsyncClient> amazonCloudWatchAsync(AWSCredentialsProvider credentialsProvider) {
		return new AmazonWebserviceClientFactoryBean<>(AmazonCloudWatchAsyncClient.class, credentialsProvider, this.regionProvider);
	}

    @Bean
    @ExportMetricWriter
	CloudWatchMetricWriter cloudWatchMetricWriter(CloudWatchMetricSender cloudWatchMetricSender) {
		return new CloudWatchMetricWriter(cloudWatchMetricSender);
	}

    @Bean
    @ConditionalOnMissingBean(CloudWatchMetricSender.class)
    CloudWatchMetricSender cloudWatchMetricWriterSender(AmazonCloudWatchAsync amazonCloudWatchAsync) {
        return new BufferingCloudWatchMetricSender(
				this.cloudWatchMetricProperties.getNamespace(),
				this.cloudWatchMetricProperties.getMaxBuffer(),
				this.cloudWatchMetricProperties.getFixedDelayBetweenRuns(),
				amazonCloudWatchAsync
        );
    }
}
