/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3;

import io.awspring.cloud.autoconfigure.core.AwsClientConfigurer;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

public class S3AwsClientConfigurerConfiguration {

	@Bean
	S3AwsClientClientConfigurer<SnsClientBuilder> snsClientBuilderAwsClientConfigurer() {
		return new S3AwsClientClientConfigurer<>();
	}

	static class S3AwsClientClientConfigurer<S3ClientBuilder> implements AwsClientConfigurer<S3ClientBuilder> {
		@Override
		@Nullable
		public ClientOverrideConfiguration overrideConfiguration() {
			return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(1542)).build();
		}

		@Override
		@Nullable
		public <T extends SdkHttpClient> SdkHttpClient httpClient() {
			return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
		}
	}
}
