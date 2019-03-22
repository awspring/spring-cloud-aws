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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 * @author Greg Turnquist
 * @since 1.0
 */
public class PathMatchingSimpleStorageResourcePatternResolverTest {

	@Test
	public void testWildcardInBucketName() throws Exception {
		AmazonS3 amazonS3 = prepareMockForTestWildcardInBucketName();

		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		assertThat(resourceLoader.getResources("s3://myBucket*/test.txt").length)
				.as("test the single '*' wildcard").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://myBucket?wo/test.txt").length)
				.as("test the '?' wildcard").isEqualTo(1);
		assertThat(resourceLoader.getResources("s3://**/test.txt").length)
				.as("test the double '**' wildcard").isEqualTo(2);
	}

	@Test
	public void testWildcardInKey() throws IOException {
		AmazonS3 amazonS3 = prepareMockForTestWildcardInKey();

		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		assertThat(resourceLoader.getResources("s3://myBucket/foo*/bar*/test.txt").length)
				.as("test the single '*' wildcard").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://myBucket/").length)
				.as("test the bucket name only").isEqualTo(1);
		assertThat(resourceLoader
				.getResources("s3://myBucke?/fooOne/ba?One/test.txt").length)
						.as("test the '?' wildcard").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://myBucket/**/test.txt").length)
				.as("test the double '**' wildcard").isEqualTo(5);
		assertThat(resourceLoader.getResources("s3://myBucke?/**/*.txt").length)
				.as("test all together").isEqualTo(5);
	}

	@Test
	public void testLoadingClasspathFile() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		Resource[] resources = resourceLoader.getResources(
				"classpath*:org/springframework/cloud/aws/core/io/s3/PathMatchingSimpleStorageResourcePatternResolverTest.class");
		assertThat(resources.length).isEqualTo(1);
		assertThat(resources[0].exists()).as("load without wildcards").isTrue();

		Resource[] resourcesWithFileNameWildcard = resourceLoader.getResources(
				"classpath*:org/**/PathMatchingSimpleStorageResourcePatternResolverTes?.class");
		assertThat(resourcesWithFileNameWildcard.length).isEqualTo(1);
		assertThat(resourcesWithFileNameWildcard[0].exists()).as("load with wildcards")
				.isTrue();
	}

	@Test
	public void testTruncatedListings() throws Exception {
		AmazonS3 amazonS3 = prepareMockForTestTruncatedListings();
		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		assertThat(resourceLoader.getResources("s3://myBucket/**/test.txt").length).as(
				"Test that all parts are returned when object summaries are truncated")
				.isEqualTo(5);
		assertThat(resourceLoader
				.getResources("s3://myBucket/fooOne/ba*/test.txt").length).as(
						"Test that all parts are return when common prefixes are truncated")
						.isEqualTo(1);
		assertThat(resourceLoader.getResources("s3://myBucket/").length)
				.as("Test that all parts are returned when only bucket name is used")
				.isEqualTo(1);
	}

	private AmazonS3 prepareMockForTestTruncatedListings() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);

		// Without prefix calls
		ObjectListing objectListingWithoutPrefixPart1 = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/test.txt"),
						createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt"),
						createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt")),
				Collections.emptyList(), true);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", null, null))))
						.thenReturn(objectListingWithoutPrefixPart1);

		ObjectListing objectListingWithoutPrefixPart2 = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooThree/baz/test.txt"),
						createS3ObjectSummaryWithKey("foFour/barFour/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listNextBatchOfObjects(objectListingWithoutPrefixPart1))
				.thenReturn(objectListingWithoutPrefixPart2);

		// With prefix calls
		ObjectListing objectListingWithPrefixPart1 = createObjectListingMock(
				Collections.emptyList(), Arrays.asList("dooOne/", "dooTwo/"), true);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", null, "/"))))
						.thenReturn(objectListingWithPrefixPart1);

		ObjectListing objectListingWithPrefixPart2 = createObjectListingMock(
				Collections.emptyList(), Collections.singletonList("fooOne/"), false);
		when(amazonS3.listNextBatchOfObjects(objectListingWithPrefixPart1))
				.thenReturn(objectListingWithPrefixPart2);

		ObjectListing objectListingWithPrefixFooOne = createObjectListingMock(
				Collections.emptyList(), Collections.singletonList("fooOne/barOne/"),
				false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/", "/"))))
						.thenReturn(objectListingWithPrefixFooOne);

		ObjectListing objectListingWithPrefixFooOneBarOne = createObjectListingMock(
				Collections.singletonList(
						createS3ObjectSummaryWithKey("fooOne/barOne/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listObjects(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooOne/barOne/", "/"))))
						.thenReturn(objectListingWithPrefixFooOneBarOne);

		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class)))
				.thenReturn(new ObjectMetadata());

		return amazonS3;
	}

	private AmazonS3 prepareMockForTestWildcardInBucketName() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.listBuckets()).thenReturn(
				Arrays.asList(new Bucket("myBucketOne"), new Bucket("myBucketTwo"),
						new Bucket("anotherBucket"), new Bucket("myBuckez")));

		// Mocks for the '**' case
		ObjectListing objectListingWithOneFile = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("test.txt")),
				Collections.emptyList(), false);
		ObjectListing emptyObjectListing = createObjectListingMock(
				Collections.emptyList(), Collections.emptyList(), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucketOne", null, null))))
						.thenReturn(objectListingWithOneFile);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucketTwo", null, null))))
						.thenReturn(emptyObjectListing);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("anotherBucket", null, null))))
						.thenReturn(objectListingWithOneFile);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBuckez", null, null))))
						.thenReturn(emptyObjectListing);

		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class)))
				.thenReturn(new ObjectMetadata());
		return amazonS3;
	}

	/**
	 * Virtual test folder structure: fooOne/barOne/test.txt fooOne/bazOne/test.txt
	 * fooTwo/barTwo/test.txt fooThree/baz/test.txt foFour/barFour/test.txt .
	 */
	private AmazonS3 prepareMockForTestWildcardInKey() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);

		// List buckets mock
		when(amazonS3.listBuckets()).thenReturn(
				Arrays.asList(new Bucket("myBucket"), new Bucket("myBuckets")));

		// Root requests
		ObjectListing objectListingMockAtRoot = createObjectListingMock(
				Collections.emptyList(),
				Arrays.asList("foFour/", "fooOne/", "fooThree/", "fooTwo/"), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", null, "/"))))
						.thenReturn(objectListingMockAtRoot);

		// Requests on fooOne
		ObjectListing objectListingFooOne = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("fooOne/")),
				Arrays.asList("fooOne/barOne/", "fooOne/bazOne/"), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/", "/"))))
						.thenReturn(objectListingFooOne);

		ObjectListing objectListingFooOneBarOne = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/"),
						createS3ObjectSummaryWithKey("fooOne/barOne/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listObjects(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooOne/barOne/", "/"))))
						.thenReturn(objectListingFooOneBarOne);

		ObjectListing objectListingFooOneBazOne = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/bazOne/"),
						createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listObjects(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooOne/bazOne/", "/"))))
						.thenReturn(objectListingFooOneBazOne);

		// Requests on fooTwo
		ObjectListing objectListingFooTwo = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("fooTwo/")),
				Collections.singletonList("fooTwo/barTwo/"), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooTwo/", "/"))))
						.thenReturn(objectListingFooTwo);

		ObjectListing objectListingFooTwoBarTwo = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooTwo/barTwo/"),
						createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listObjects(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooTwo/barTwo/", "/"))))
						.thenReturn(objectListingFooTwoBarTwo);

		// Requests on fooThree
		ObjectListing objectListingFooThree = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("fooThree/")),
				Collections.singletonList("fooTwo/baz/"), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooThree/", "/"))))
						.thenReturn(objectListingFooThree);

		ObjectListing objectListingFooThreeBaz = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooThree/baz/"),
						createS3ObjectSummaryWithKey("fooThree/baz/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooThree/baz/", "/"))))
						.thenReturn(objectListingFooThreeBaz);

		// Requests for foFour
		ObjectListing objectListingFoFour = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("foFour/")),
				Collections.singletonList("foFour/barFour/"), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", "foFour/", "/"))))
						.thenReturn(objectListingFoFour);

		ObjectListing objectListingFoFourBarFour = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("foFour/barFour/"),
						createS3ObjectSummaryWithKey("foFour/barFour/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listObjects(argThat(
				new ListObjectsRequestMatcher("myBucket", "foFour/barFour/", "/"))))
						.thenReturn(objectListingFoFourBarFour);

		// Requests for all
		ObjectListing fullObjectListing = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/test.txt"),
						createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt"),
						createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt"),
						createS3ObjectSummaryWithKey("fooThree/baz/test.txt"),
						createS3ObjectSummaryWithKey("foFour/barFour/test.txt")),
				Collections.emptyList(), false);
		when(amazonS3.listObjects(
				argThat(new ListObjectsRequestMatcher("myBucket", null, null))))
						.thenReturn(fullObjectListing);

		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class)))
				.thenReturn(new ObjectMetadata());

		return amazonS3;
	}

	private ObjectListing createObjectListingMock(List<S3ObjectSummary> objectSummaries,
			List<String> commonPrefixes, boolean truncated) {
		ObjectListing objectListing = mock(ObjectListing.class);
		when(objectListing.getObjectSummaries()).thenReturn(objectSummaries);
		when(objectListing.getCommonPrefixes()).thenReturn(commonPrefixes);
		when(objectListing.isTruncated()).thenReturn(truncated);
		return objectListing;
	}

	private S3ObjectSummary createS3ObjectSummaryWithKey(String key) {
		S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
		s3ObjectSummary.setKey(key);
		return s3ObjectSummary;
	}

	private ResourcePatternResolver getResourceLoader(AmazonS3 amazonS3) {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(new SimpleStorageProtocolResolver(amazonS3));
		return new PathMatchingSimpleStorageResourcePatternResolver(amazonS3,
				new PathMatchingResourcePatternResolver(loader));
	}

	private static final class ListObjectsRequestMatcher
			implements ArgumentMatcher<ListObjectsRequest> {

		private final String bucketName;

		private final String prefix;

		private final String delimiter;

		private ListObjectsRequestMatcher(String bucketName, String prefix,
				String delimiter) {
			this.bucketName = bucketName;
			this.prefix = prefix;
			this.delimiter = delimiter;
		}

		@Override
		public boolean matches(ListObjectsRequest listObjectsRequest) {
			if (listObjectsRequest == null) {
				return false;
			}
			boolean bucketNameIsEqual;
			if (listObjectsRequest.getBucketName() != null) {
				bucketNameIsEqual = listObjectsRequest.getBucketName()
						.equals(this.bucketName);
			}
			else {
				bucketNameIsEqual = this.bucketName == null;
			}

			boolean prefixIsEqual;
			if (listObjectsRequest.getPrefix() != null) {
				prefixIsEqual = listObjectsRequest.getPrefix().equals(this.prefix);
			}
			else {
				prefixIsEqual = this.prefix == null;
			}

			boolean delimiterIsEqual;
			if (listObjectsRequest.getDelimiter() != null) {
				delimiterIsEqual = listObjectsRequest.getDelimiter()
						.equals(this.delimiter);
			}
			else {
				delimiterIsEqual = this.delimiter == null;
			}

			return delimiterIsEqual && prefixIsEqual && bucketNameIsEqual;
		}

	}

}
