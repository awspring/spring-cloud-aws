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

package io.awspring.cloud.s3.crossregion;

import java.util.HashMap;
import java.util.Map;

import io.awspring.cloud.s3.crossregion.CrossRegionS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CrossRegionS3Client}. Integration testing with Localstack is not
 * possible due to: https://github.com/localstack/localstack/issues/5748
 *
 * @author Maciej Walkowiak
 */
class CrossRegionS3ClientTests {

	private final S3ClientBuilder mock = mock(S3ClientBuilder.class);

	/**
	 * The default client.
	 */
	private final S3Client defaultClient = mock(S3Client.class);

	/**
	 * Clients per region.
	 */
	private final Map<Region, S3Client> clients = new HashMap<>();

	private CrossRegionS3Client crossRegionS3Client;

	@BeforeEach
	void beforeEach() {
		Region.regions().forEach(region -> clients.put(region, mock(S3Client.class)));
		Map<Region, S3ClientBuilder> builders = new HashMap<>();
		Region.regions().forEach(region -> {
			S3ClientBuilder mock = mock(S3ClientBuilder.class, Answers.RETURNS_DEEP_STUBS);
			when(mock.build()).thenReturn(clients.get(region));
			builders.put(region, mock);
		});
		when(mock.region(any()))
				.thenAnswer(invocationOnMock -> builders.get(invocationOnMock.getArgument(0, Region.class)));

		when(mock.build()).thenReturn(defaultClient);
		crossRegionS3Client = new CrossRegionS3Client(mock);
	}

	@Test
	void usesRegionSpecificClientToAccessBucket() {
		createBucket("first-bucket", Region.EU_WEST_2);

		crossRegionS3Client.listObjects(r -> r.bucket("first-bucket"));

		// eu-west-2 client is used to list objects from "first-bucket"
		verify(clients.get(Region.EU_WEST_2)).listObjects(ListObjectsRequest.builder().bucket("first-bucket").build());

		// no integrations with eu-west-1
		verifyNoInteractions(clients.get(Region.EU_WEST_1));

		// eu-west-2 is cached
		assertThat(crossRegionS3Client.getClientCache().get(Region.EU_WEST_2)).isEqualTo(clients.get(Region.EU_WEST_2));
		assertThat(crossRegionS3Client.getClientCache()).hasSize(1);
	}

	@Test
	void usesClientCachedPerRegion() {
		createBucket("first-bucket", Region.EU_WEST_2);
		createBucket("second-bucket", Region.EU_WEST_2);
		createBucket("us-west-1-bucket", Region.US_WEST_1);

		crossRegionS3Client.listObjects(r -> r.bucket("first-bucket"));
		crossRegionS3Client.listObjects(r -> r.bucket("second-bucket"));
		crossRegionS3Client.listObjects(r -> r.bucket("us-west-1-bucket"));

		// ensure client for eu-west-2 region was created only once
		verify(mock.region(Region.EU_WEST_2)).build();
		verify(mock.region(Region.US_WEST_1)).build();
		verify(mock.region(Region.EU_WEST_1), never()).build();

		assertThat(crossRegionS3Client.getClientCache().get(Region.EU_WEST_2)).isEqualTo(clients.get(Region.EU_WEST_2));
		assertThat(crossRegionS3Client.getClientCache().get(Region.US_WEST_1)).isEqualTo(clients.get(Region.US_WEST_1));
		assertThat(crossRegionS3Client.getClientCache()).hasSize(2);
	}

	@Test
	void usesCachedBucketLocation() {
		createBucket("first-bucket", Region.EU_WEST_2);

		crossRegionS3Client.listObjects(r -> r.bucket("first-bucket"));
		crossRegionS3Client.listObjects(r -> r.bucket("first-bucket"));

		// ensure bucket location was requested only once
		verify(defaultClient).getBucketLocation(GetBucketLocationRequest.builder().bucket("first-bucket").build());
	}

	private void createBucket(String s, Region region) {
		when(defaultClient.listObjects(ListObjectsRequest.builder().bucket(s).build())).thenThrow(S3Exception.builder()
				.awsErrorDetails(AwsErrorDetails.builder().errorCode("PermanentRedirect").build()).build());
		when(defaultClient.getBucketLocation(GetBucketLocationRequest.builder().bucket(s).build()))
				.thenReturn(GetBucketLocationResponse.builder().locationConstraint(region.id()).build());
	}

}
