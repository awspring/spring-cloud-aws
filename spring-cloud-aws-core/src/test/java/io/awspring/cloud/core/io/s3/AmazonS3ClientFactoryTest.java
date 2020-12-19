/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.core.io.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Agim Emruli
 */
class AmazonS3ClientFactoryTest {

	@Test
	void createClientForEndpointUrl_withNullEndpoint_throwsIllegalArgumentException() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();

		// Assert
		assertThatThrownBy(() -> amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Endpoint Url must not be null");

	}

	@Test
	void createClientForEndpointUrl_withNullAmazonS3Client_throwsIllegalArgumentException() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();

		// Assert
		assertThatThrownBy(() -> amazonS3ClientFactory.createClientForEndpointUrl(null, "https://s3.amazonaws.com"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("AmazonS3 must not be null");

	}

	@Test
	void createClientForEndpointUrl_withDefaultRegionUrl_createClientForDefaultRegion() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();

		// Act
		AmazonS3 newClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, "https://s3.amazonaws.com");

		// Prepare
		assertThat(newClient.getRegionName()).isEqualTo(Regions.DEFAULT_REGION.getName());
	}

	@Test
	void createClientForEndpointUrl_withProvidedBucketRegion_createClientForDefaultRegion() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();

		// Act
		AmazonS3 newClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3, "https://s3.amazonaws.com",
				Regions.US_EAST_1);

		// Prepare
		assertThat(newClient.getRegionName()).isEqualTo(Regions.US_EAST_1.getName());
	}

	@Test
	void createClientForEndpointUrl_withCustomRegionUrl_createClientForCustomRegion() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();

		// Act
		AmazonS3 newClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3,
				"https://myBucket.s3.eu-central-1.amazonaws.com");

		// Prepare
		assertThat(newClient.getRegionName()).isEqualTo(Regions.EU_CENTRAL_1.getName());
	}

	@Test
	void createClientForEndpointUrl_withProxiedClient_createClientForCustomRegion() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		AmazonS3 amazonS3 = AmazonS3ProxyFactory
				.createProxy(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build());

		// Act
		AmazonS3 newClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3,
				"https://myBucket.s3.eu-central-1.amazonaws.com");

		// Prepare
		assertThat(newClient.getRegionName()).isEqualTo(Regions.EU_CENTRAL_1.getName());
	}

	@Test
	void createClientForEndpointUrl_withCustomRegionUrlAndCachedClient_returnsCachedClient() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();

		AmazonS3 existingClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3,
				"https://myBucket.s3.eu-central-1.amazonaws.com");

		// Act
		AmazonS3 cachedClient = amazonS3ClientFactory.createClientForEndpointUrl(amazonS3,
				"https://myBucket.s3.eu-central-1.amazonaws.com");

		// Prepare
		assertThat(existingClient).isSameAs(cachedClient);
	}

}
