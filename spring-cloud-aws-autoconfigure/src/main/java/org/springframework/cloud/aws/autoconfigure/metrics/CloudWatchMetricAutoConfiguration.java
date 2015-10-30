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

package org.springframework.cloud.aws.autoconfigure.metrics;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Autoconfiguration which creates and exports a {@link CloudWatchMetricWriter} alongside
 * with an {@link AmazonCloudWatchAsync} if its not already present.
 *
 * @author Simon Buettner
 */
@Configuration
@Import(ContextCredentialsAutoConfiguration.class)
@EnableConfigurationProperties(CloudWatchMetricProperties.class)
@ConditionalOnProperty(prefix = "cloud.aws.cloudwatch", name = "namespace")
public class CloudWatchMetricAutoConfiguration {

    @Autowired(required = false)
    private RegionProvider regionProvider;

    @Autowired
    CloudWatchMetricProperties cloudWatchMetricProperties;

    @Bean
    @ConditionalOnMissingAmazonClient(AmazonCloudWatchAsync.class)
    public AmazonCloudWatchAsync amazonCloudWatchAsync(AWSCredentialsProvider credentialsProvider) {
        AmazonCloudWatchAsyncClient serviceClient = new AmazonCloudWatchAsyncClient(credentialsProvider);
        if (this.regionProvider != null) {
            serviceClient.setRegion(this.regionProvider.getRegion());
        }
        return serviceClient;
    }

    @Bean
    @ExportMetricWriter
    CloudWatchMetricWriter cloudWatchMetricWriter(CloudWatchMetricSender cloudWatchMetricSender) {
        CloudWatchMetricWriter cloudWatchMetricWriter = new CloudWatchMetricWriter(cloudWatchMetricSender);
        return cloudWatchMetricWriter;
    }

    @Bean
    @ConditionalOnMissingBean(CloudWatchMetricSender.class)
    CloudWatchMetricSender cloudWatchMetricWriterSender(AmazonCloudWatchAsync amazonCloudWatchAsync) {
        return new BufferingCloudWatchMetricSender(
            cloudWatchMetricProperties.getNamespace(),
            cloudWatchMetricProperties.getMaxBuffer(),
            cloudWatchMetricProperties.getNextRunDelayMillis(),
            amazonCloudWatchAsync
        );
    }

}
