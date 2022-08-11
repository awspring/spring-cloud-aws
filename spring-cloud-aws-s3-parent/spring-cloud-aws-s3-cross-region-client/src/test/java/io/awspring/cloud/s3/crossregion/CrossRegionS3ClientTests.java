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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Unit tests for {@link CrossRegionS3Client}. Integration testing with Localstack is not possible due to:
 * https://github.com/localstack/localstack/issues/5748
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

		// ensure defaultClient is only used the first time.
		verify(defaultClient, times(1)).listObjects(any(ListObjectsRequest.class));
		verify(clients.get(Region.EU_WEST_2), times(2)).listObjects(any(ListObjectsRequest.class));
	}

	@Test
	void exceptionIfRegionHeaderMissingOnHeadBucket() {
		when(defaultClient.headBucket(any(Consumer.class))).thenCallRealMethod();
		when(defaultClient.headBucket(any(HeadBucketRequest.class)))
				.thenThrow(
						S3Exception.builder()
								.awsErrorDetails(AwsErrorDetails.builder()
										.sdkHttpResponse(SdkHttpResponse.builder().statusCode(301).build()).build())
								.build());
		assertThatThrownBy(() -> crossRegionS3Client.headBucket(r -> r.bucket("first-bucket")))
				.isInstanceOf(CrossRegionS3Client.RegionDiscoveryException.class);
		verify(defaultClient, times(1)).headBucket(HeadBucketRequest.builder().bucket("first-bucket").build());
	}

	@Test
	void exceptionPassthroughIfRegionIrrelevant() {
		AwsServiceException exceptionToThrow = S3Exception.builder().awsErrorDetails(
				AwsErrorDetails.builder().sdkHttpResponse(SdkHttpResponse.builder().statusCode(404).build()).build())
				.build();
		when(defaultClient.listObjects(ListObjectsRequest.builder().bucket("first-bucket").build()))
				.thenThrow(exceptionToThrow);

		assertThatThrownBy(() -> crossRegionS3Client.listObjects(r -> r.bucket("first-bucket")))
				.isEqualTo(exceptionToThrow);
	}

	@Test
	void headBucketUsedWhenHeaderMissing() {
		createBucket("first-bucket", Region.EU_WEST_2);
		crossRegionS3Client.createBucket(r -> r.bucket("first-bucket"));
		verify(defaultClient, times(1)).headBucket(HeadBucketRequest.builder().bucket("first-bucket").build());
		verify(defaultClient, times(1)).createBucket(CreateBucketRequest.builder().bucket("first-bucket").build());
	}

	private void createBucket(String s, Region region) {
		when(defaultClient.listObjects(any(Consumer.class))).thenCallRealMethod();
		when(defaultClient.listObjects(ListObjectsRequest.builder().bucket(s).build())).thenThrow(S3Exception.builder()
				.awsErrorDetails(AwsErrorDetails.builder()
						.sdkHttpResponse(SdkHttpResponse.builder().statusCode(301)
								.appendHeader(CrossRegionS3Client.BUCKET_REDIRECT_HEADER, region.id()).build())
						.build())
				.build());
		when(defaultClient.headBucket(any(Consumer.class))).thenCallRealMethod();
		when(defaultClient.headBucket(HeadBucketRequest.builder().bucket(s).build())).thenThrow(S3Exception.builder()
				.awsErrorDetails(AwsErrorDetails.builder()
						.sdkHttpResponse(SdkHttpResponse.builder().statusCode(301)
								.appendHeader(CrossRegionS3Client.BUCKET_REDIRECT_HEADER, region.id()).build())
						.build())
				.build());
		when(defaultClient.createBucket(any(Consumer.class))).thenCallRealMethod();
		when(defaultClient.createBucket(CreateBucketRequest.builder().bucket(s).build()))
				.thenThrow(
						S3Exception.builder()
								.awsErrorDetails(AwsErrorDetails.builder()
										.sdkHttpResponse(SdkHttpResponse.builder().statusCode(301).build()).build())
								.build());
	}

}
