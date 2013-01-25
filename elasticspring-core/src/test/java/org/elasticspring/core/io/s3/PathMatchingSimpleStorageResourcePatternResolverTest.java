/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class PathMatchingSimpleStorageResourcePatternResolverTest {

	@Test
	public void testWildcardInBucketName() throws Exception {
		AmazonS3 amazonS3 = prepareMockForTestWildcardInBucketName();

		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		// (test the single '*' wildcard)
		{
			Resource[] resources = resourceLoader.getResources("s3://myBucket*/test.txt");
			Assert.assertEquals(2, resources.length);
		}

		// (test the '?' wildcard)
		{
			Resource[] resources = resourceLoader.getResources("s3://myBucket?wo/test.txt");
			Assert.assertEquals(1, resources.length);
		}

		// (test the double '**' wildcard)
		{
			Resource[] resources = resourceLoader.getResources("s3://**/test.txt");
			Assert.assertEquals(4, resources.length);
		}
	}

	@Test
	public void testWildcardInKey() throws IOException {
		AmazonS3 amazonS3 = prepareMockForTestWildcardInKey();

		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		// (test the single '*' wildcard)
		{
			Resource[] resources = resourceLoader.getResources("s3://myBucket/foo*/bar*/test.txt");
			Assert.assertEquals(2, resources.length);
		}

		// (test the '?' wildcard)
		{
			Resource[] resources = resourceLoader.getResources("s3://myBucke?/fooOne/ba?One/test.txt");
			Assert.assertEquals(2, resources.length);
		}

		// (test the double '**' wildcard)
		{
			Resource[] resources = resourceLoader.getResources("s3://myBucket/fooOne/**/test.txt");
			Assert.assertEquals(2, resources.length);
		}

		// (test all together)
		{
			Resource[] resources = resourceLoader.getResources("s3://myBucke?/fooO*/**/te?t.txt");
			Assert.assertEquals(2, resources.length);
		}
	}

	@Test
	public void testLoadingClasspathFile() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		// (load without wildcards)
		{
			Resource[] resources = resourceLoader.getResources("classpath*:org/elasticspring/core/io/s3/PathMatchingSimpleStorageResourcePatternResolverTest.class");
			Assert.assertEquals(1, resources.length);
			Assert.assertTrue(resources[0].exists());
		}

		// (load with wildcards)
		{
			Resource[] resources = resourceLoader.getResources("classpath*:org/**/core/i*/s3/PathMatchingSimpleStorageResourcePatternResolverTes?.class");
			Assert.assertEquals(1, resources.length);
			Assert.assertTrue(resources[0].exists());
		}
	}

	private AmazonS3 prepareMockForTestWildcardInBucketName() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.listBuckets()).thenReturn(Arrays.asList(new Bucket("myBucketOne"), new Bucket("myBucketTwo"),
				new Bucket("anotherBucket"), new Bucket("myBuckez")));
		when(amazonS3.getObjectMetadata(anyString(), anyString())).thenReturn(new ObjectMetadata());
		return amazonS3;
	}

	/**
	 * Virtual test folder structure:
	 * fooOne/barOne/test.txt
	 * fooOne/bazOne/test.txt
	 * fooTwo/barTwo/test.txt
	 * fooThree/baz/test.txt
	 * foFour/barFour/test.txt
	 */
	private AmazonS3 prepareMockForTestWildcardInKey() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);

		// List buckets mock
		when(amazonS3.listBuckets()).thenReturn(Arrays.asList(new Bucket("myBucket"), new Bucket("myBuckets")));

		// Root requests
		ObjectListing objectListingMockAtRoot = createObjectListingMock(Collections.<S3ObjectSummary>emptyList(), Arrays.asList("foFour/", "fooOne/", "fooThree/", "fooTwo/"));
		when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher(null)))).thenReturn(objectListingMockAtRoot);

		// Requests on fooOne
		ObjectListing objectListingFooOne = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/")), Arrays.asList("fooOne/barOne/", "fooOne/bazOne/"));
		when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("fooOne/")))).thenReturn(objectListingFooOne);

		ObjectListing objectListingFooOneBarOne = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/"),
				createS3ObjectSummaryWithKey("fooOne/barOne/test.txt")), Collections.<String>emptyList());
		when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("fooOne/barOne/")))).thenReturn(objectListingFooOneBarOne);

		ObjectListing objectListingFooOneBazOne = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/bazOne/"),
				createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt")), Collections.<String>emptyList());
		when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("fooOne/bazOne/")))).thenReturn(objectListingFooOneBazOne);

		// Requests on fooTwo
		ObjectListing objectListingFooTwo = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooTwo/")), Arrays.asList("fooTwo/barTwo/"));
		when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("fooTwo/")))).thenReturn(objectListingFooTwo);

		ObjectListing objectListingFooTwoBarTwo = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooTwo/barTwo/"), createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt")), Collections.<String>emptyList());
		when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("fooTwo/barTwo/")))).thenReturn(objectListingFooTwoBarTwo);

		// Requests on fooThree
		ObjectListing objectListingFooThree = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooThree/")), Arrays.asList("fooTwo/baz/"));
		when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("fooThree/")))).thenReturn(objectListingFooThree);

		when(amazonS3.getObjectMetadata(anyString(), anyString())).thenReturn(new ObjectMetadata());

		return amazonS3;
	}

	private ObjectListing createObjectListingMock(List<S3ObjectSummary> objectSummaries, List<String> commonPrefixes) {
		ObjectListing objectListing = mock(ObjectListing.class);
		when(objectListing.getObjectSummaries()).thenReturn(objectSummaries);
		when(objectListing.getCommonPrefixes()).thenReturn(commonPrefixes);
		return objectListing;
	}

	private S3ObjectSummary createS3ObjectSummaryWithKey(String key) {
		S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
		s3ObjectSummary.setKey(key);
		return s3ObjectSummary;
	}

	private ResourcePatternResolver getResourceLoader(AmazonS3 amazonS3) {
		return new PathMatchingSimpleStorageResourcePatternResolver(amazonS3);
	}

	private static class ListObjectsRequestMatcher extends ArgumentMatcher<ListObjectsRequest> {

		private final String prefix;

		private ListObjectsRequestMatcher(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public boolean matches(Object argument) {
			if (argument instanceof ListObjectsRequest) {
				ListObjectsRequest listObjectsRequest = (ListObjectsRequest) argument;
				if (listObjectsRequest.getPrefix() != null) {
					return listObjectsRequest.getPrefix().equals(this.prefix);
				} else {
					return this.prefix == null;
				}
			} else {
				return false;
			}
		}
	}

}
