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

package org.springframework.cloud.aws.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

        assertEquals("test the single '*' wildcard", 2, resourceLoader.getResources("s3://myBucket*/test.txt").length);
        assertEquals("test the '?' wildcard", 1, resourceLoader.getResources("s3://myBucket?wo/test.txt").length);
        assertEquals("test the double '**' wildcard", 2, resourceLoader.getResources("s3://**/test.txt").length);
    }

    @Test
    public void testWildcardInKey() throws IOException {
        AmazonS3 amazonS3 = prepareMockForTestWildcardInKey();

        ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

        assertEquals("test the single '*' wildcard", 2, resourceLoader.getResources("s3://myBucket/foo*/bar*/test.txt").length);
        assertEquals("test the '?' wildcard", 2, resourceLoader.getResources("s3://myBucke?/fooOne/ba?One/test.txt").length);
        assertEquals("test the double '**' wildcard", 5, resourceLoader.getResources("s3://myBucket/**/test.txt").length);
        assertEquals("test all together", 5, resourceLoader.getResources("s3://myBucke?/**/*.txt").length);
    }

    @Test
    public void testLoadingClasspathFile() throws Exception {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

        Resource[] resources = resourceLoader.getResources("classpath*:org/springframework/cloud/aws/core/io/s3/PathMatchingSimpleStorageResourcePatternResolverTest.class");
        assertEquals(1, resources.length);
        assertTrue("load without wildcards", resources[0].exists());

        Resource[] resourcesWithFileNameWildcard = resourceLoader.getResources("classpath*:org/**/PathMatchingSimpleStorageResourcePatternResolverTes?.class");
        assertEquals(1, resourcesWithFileNameWildcard.length);
        assertTrue("load with wildcards", resourcesWithFileNameWildcard[0].exists());
    }

    @Test
    public void testTruncatedListings() throws Exception {
        AmazonS3 amazonS3 = prepareMockForTestTruncatedListings();
        ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

        assertEquals("Test that all parts are returned when object summaries are truncated", 5, resourceLoader.getResources("s3://myBucket/**/test.txt").length);
        assertEquals("Test that all parts are return when common prefixes are truncated", 1, resourceLoader.getResources("s3://myBucket/fooOne/ba*/test.txt").length);
    }

    @Test
    public void testWithCustomPathMatcher() throws Exception {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        PathMatcher pathMatcher = mock(PathMatcher.class);

        PathMatchingSimpleStorageResourcePatternResolver patternResolver = new PathMatchingSimpleStorageResourcePatternResolver(amazonS3,
                new SimpleStorageResourceLoader(amazonS3),
                new PathMatchingResourcePatternResolver());
        patternResolver.setPathMatcher(pathMatcher);

        patternResolver.getResources("s3://foo/bar");

        verify(pathMatcher, times(1)).isPattern("foo/bar");
    }

    @Test
    public void testWithRedirectError() throws Exception {
        AmazonS3 amazonS3 = prepareMocksForRedirectError();

        ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);
        Resource[] resources = resourceLoader.getResources("s3://spring.io/*.txt");
        assertThat(resources.length, is(1));
        assertThat(resources[0].contentLength(), is(5L));
    }

    private AmazonS3 prepareMocksForRedirectError() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);

        AmazonS3Exception exception = new AmazonS3Exception("Mock error", "<Error><EndPoint>new.path.com</EndPoint></Error>");
        exception.setStatusCode(301);

        S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
        s3ObjectSummary.setKey("stub_key.txt");
        s3ObjectSummary.setBucketName("spring.io");

        ObjectListing objectListing = new ObjectListing();
        objectListing.setBucketName("spring.io");
        objectListing.getObjectSummaries().add(s3ObjectSummary);

        when(amazonS3.listObjects(any(ListObjectsRequest.class)))
                .thenThrow(exception).thenReturn(objectListing);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(5);

        when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(objectMetadata);

        return amazonS3;
    }

    private AmazonS3 prepareMockForTestTruncatedListings() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);

        // Without prefix calls
        ObjectListing objectListingWithoutPrefixPart1 = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/test.txt"),
                createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt"), createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt")), Collections.<String>emptyList(), true);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", null, null)))).thenReturn(objectListingWithoutPrefixPart1);

        ObjectListing objectListingWithoutPrefixPart2 = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooThree/baz/test.txt"),
                createS3ObjectSummaryWithKey("foFour/barFour/test.txt")), Collections.<String>emptyList(), false);
        when(amazonS3.listNextBatchOfObjects(objectListingWithoutPrefixPart1)).thenReturn(objectListingWithoutPrefixPart2);

        // With prefix calls
        ObjectListing objectListingWithPrefixPart1 = createObjectListingMock(Collections.<S3ObjectSummary>emptyList(), Arrays.asList("dooOne/", "dooTwo/"), true);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", null, "/")))).thenReturn(objectListingWithPrefixPart1);

        ObjectListing objectListingWithPrefixPart2 = createObjectListingMock(Collections.<S3ObjectSummary>emptyList(), Arrays.asList("fooOne/"), false);
        when(amazonS3.listNextBatchOfObjects(objectListingWithPrefixPart1)).thenReturn(objectListingWithPrefixPart2);

        ObjectListing objectListingWithPrefixFooOne = createObjectListingMock(Collections.<S3ObjectSummary>emptyList(), Arrays.asList("fooOne/barOne/"), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/", "/")))).thenReturn(objectListingWithPrefixFooOne);

        ObjectListing objectListingWithPrefixFooOneBarOne = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/test.txt")), Collections.<String>emptyList(), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/barOne/", "/")))).thenReturn(objectListingWithPrefixFooOneBarOne);

        when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());

        return amazonS3;
    }

    private AmazonS3 prepareMockForTestWildcardInBucketName() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        when(amazonS3.listBuckets()).thenReturn(Arrays.asList(new Bucket("myBucketOne"), new Bucket("myBucketTwo"),
                new Bucket("anotherBucket"), new Bucket("myBuckez")));

        // Mocks for the '**' case
        ObjectListing objectListingWithOneFile = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("test.txt")), Collections.<String>emptyList(), false);
        ObjectListing emptyObjectListing = createObjectListingMock(Collections.<S3ObjectSummary>emptyList(), Collections.<String>emptyList(), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucketOne", null, null)))).thenReturn(objectListingWithOneFile);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucketTwo", null, null)))).thenReturn(emptyObjectListing);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("anotherBucket", null, null)))).thenReturn(objectListingWithOneFile);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBuckez", null, null)))).thenReturn(emptyObjectListing);

        when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());
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
        ObjectListing objectListingMockAtRoot = createObjectListingMock(Collections.<S3ObjectSummary>emptyList(), Arrays.asList("foFour/", "fooOne/", "fooThree/", "fooTwo/"), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", null, "/")))).thenReturn(objectListingMockAtRoot);

        // Requests on fooOne
        ObjectListing objectListingFooOne = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/")), Arrays.asList("fooOne/barOne/", "fooOne/bazOne/"), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/", "/")))).thenReturn(objectListingFooOne);

        ObjectListing objectListingFooOneBarOne = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/"),
                createS3ObjectSummaryWithKey("fooOne/barOne/test.txt")), Collections.<String>emptyList(), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/barOne/", "/")))).thenReturn(objectListingFooOneBarOne);

        ObjectListing objectListingFooOneBazOne = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/bazOne/"),
                createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt")), Collections.<String>emptyList(), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/bazOne/", "/")))).thenReturn(objectListingFooOneBazOne);

        // Requests on fooTwo
        ObjectListing objectListingFooTwo = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooTwo/")), Arrays.asList("fooTwo/barTwo/"), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooTwo/", "/")))).thenReturn(objectListingFooTwo);

        ObjectListing objectListingFooTwoBarTwo = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooTwo/barTwo/"),
                createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt")), Collections.<String>emptyList(), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooTwo/barTwo/", "/")))).thenReturn(objectListingFooTwoBarTwo);

        // Requests on fooThree
        ObjectListing objectListingFooThree = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooThree/")), Arrays.asList("fooTwo/baz/"), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooThree/", "/")))).thenReturn(objectListingFooThree);

        ObjectListing objectListingFooThreeBaz = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooThree/baz/"),
                createS3ObjectSummaryWithKey("fooThree/baz/test.txt")), Collections.<String>emptyList(), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "fooThree/baz/", "/")))).thenReturn(objectListingFooThreeBaz);

        // Requests for foFour
        ObjectListing objectListingFoFour = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("foFour/")), Arrays.asList("foFour/barFour/"), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "foFour/", "/")))).thenReturn(objectListingFoFour);

        ObjectListing objectListingFoFourBarFour = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("foFour/barFour/"),
                createS3ObjectSummaryWithKey("foFour/barFour/test.txt")), Collections.<String>emptyList(), false);
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", "foFour/barFour/", "/")))).thenReturn(objectListingFoFourBarFour);

        // Requests for all
        ObjectListing fullObjectListing = createObjectListingMock(Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/test.txt"),
                createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt"), createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt"),
                createS3ObjectSummaryWithKey("fooThree/baz/test.txt"), createS3ObjectSummaryWithKey("foFour/barFour/test.txt")),
                Collections.<String>emptyList(), false
        );
        when(amazonS3.listObjects(argThat(new ListObjectsRequestMatcher("myBucket", null, null)))).thenReturn(fullObjectListing);

        when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());

        return amazonS3;
    }

    private ObjectListing createObjectListingMock(List<S3ObjectSummary> objectSummaries, List<String> commonPrefixes, boolean truncated) {
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
        return new PathMatchingSimpleStorageResourcePatternResolver(amazonS3, new SimpleStorageResourceLoader(amazonS3),
                new PathMatchingResourcePatternResolver());
    }

    private static class ListObjectsRequestMatcher extends ArgumentMatcher<ListObjectsRequest> {

        private final String bucketName;
        private final String prefix;
        private final String delimiter;

        private ListObjectsRequestMatcher(String bucketName, String prefix, String delimiter) {
            this.bucketName = bucketName;
            this.prefix = prefix;
            this.delimiter = delimiter;
        }

        @Override
        public boolean matches(Object argument) {
            if (argument instanceof ListObjectsRequest) {
                ListObjectsRequest listObjectsRequest = (ListObjectsRequest) argument;
                boolean bucketNameIsEqual;
                if (listObjectsRequest.getBucketName() != null) {
                    bucketNameIsEqual = listObjectsRequest.getBucketName().equals(this.bucketName);
                } else {
                    bucketNameIsEqual = this.bucketName == null;
                }

                boolean prefixIsEqual;
                if (listObjectsRequest.getPrefix() != null) {
                    prefixIsEqual = listObjectsRequest.getPrefix().equals(this.prefix);
                } else {
                    prefixIsEqual = this.prefix == null;
                }

                boolean delimiterIsEqual;
                if (listObjectsRequest.getDelimiter() != null) {
                    delimiterIsEqual = listObjectsRequest.getDelimiter().equals(this.delimiter);
                } else {
                    delimiterIsEqual = this.delimiter == null;
                }


                return delimiterIsEqual && prefixIsEqual && bucketNameIsEqual;
            } else {
                return false;
            }
        }
    }

}
