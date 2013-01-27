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
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class PathMatchingSimpleStorageResourcePatternResolver implements ResourcePatternResolver {

	private final AmazonS3 amazonS3;
	private final ResourceLoader simpleStorageResourceLoader;
	private final ResourcePatternResolver resourcePatternResolverDelegate;
	private PathMatcher pathMatcher = new AntPathMatcher();
	private static final String S3_PROTOCOL_PREFIX = "s3://";


	public PathMatchingSimpleStorageResourcePatternResolver(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
		this.simpleStorageResourceLoader = new SimpleStorageResourceLoader(amazonS3);
		this.resourcePatternResolverDelegate = new PathMatchingResourcePatternResolver();
	}

	@SuppressWarnings("UnusedDeclaration")
	public PathMatchingSimpleStorageResourcePatternResolver(AmazonS3 amazonS3, ClassLoader classLoader) {
		this.amazonS3 = amazonS3;
		this.simpleStorageResourceLoader = new SimpleStorageResourceLoader(amazonS3, classLoader);
		this.resourcePatternResolverDelegate = new PathMatchingResourcePatternResolver(classLoader);
	}

	public PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		if (locationPattern.startsWith(S3_PROTOCOL_PREFIX)) {
			int prefixEnd = locationPattern.indexOf(":") + 3;
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				return findPathMatchingResources(locationPattern);
			} else {
				return new Resource[]{this.simpleStorageResourceLoader.getResource(locationPattern)};
			}
		} else {
			return this.resourcePatternResolverDelegate.getResources(locationPattern);
		}
	}

	protected Resource[] findPathMatchingResources(String locationPattern) {
		String bucketPattern = getBucketPattern(locationPattern);
		String keyPattern = locationPattern.substring(S3_PROTOCOL_PREFIX.length() + bucketPattern.length() + 1);
		Set<Resource> resources;
		if (this.pathMatcher.isPattern(bucketPattern)) {
			List<String> matchingBuckets = findMatchingBuckets(bucketPattern);
			if (bucketPattern.startsWith("**")) {
				keyPattern = "**/" + keyPattern;
			}
			resources = findPathMatchingKeys(keyPattern, matchingBuckets);
		} else {
			resources = findPathMatchingKeys(keyPattern, Arrays.asList(bucketPattern));
		}

		return resources.toArray(new Resource[resources.size()]);
	}

	private Set<Resource> findPathMatchingKeys(String keyPattern, List<String> matchingBuckets) {
		Set<Resource> resources = new HashSet<Resource>();
		if (this.pathMatcher.isPattern(keyPattern)) {
			for (String bucketName : matchingBuckets) {
				findPathMatchingKeyInBucket(bucketName, resources, null, keyPattern);
			}
		} else {
			for (String matchingBucket : matchingBuckets) {
				Resource resource = this.simpleStorageResourceLoader.getResource(S3_PROTOCOL_PREFIX + matchingBucket + "/" + keyPattern);
				if (resource.exists()) {
					resources.add(resource);
				}
			}
		}
		return resources;
	}

	private void findPathMatchingKeyInBucket(String bucketName, Set<Resource> resources, String prefix, String keyPattern) {
		String remainingPatternPart = getRemainingPatternPart(keyPattern, prefix);
		if (remainingPatternPart.startsWith("**")) {
			findAllResourcesThatMatches(bucketName, resources, prefix, keyPattern);
		} else {
			findProgressivelyWithParitalMatch(bucketName, resources, prefix, keyPattern);
		}
	}

	private void findAllResourcesThatMatches(String bucketName, Set<Resource> resources, String prefix, String keyPattern) {
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
		ObjectListing objectListing = null;

		do {
			if (objectListing == null) {
				objectListing = this.amazonS3.listObjects(listObjectsRequest);
			} else {
				objectListing = this.amazonS3.listNextBatchOfObjects(objectListing);
			}

			Set<Resource> newResources = getResourcesFromObjectSummaries(bucketName, objectListing.getObjectSummaries(), keyPattern);
			if (newResources.size() > 0) {
				resources.addAll(newResources);
			}
		} while (objectListing.isTruncated());
	}

	private void findProgressivelyWithParitalMatch(String bucketName, Set<Resource> resources, String prefix, String keyPattern) {
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withDelimiter("/").withPrefix(prefix);
		ObjectListing objectListing = null;

		do {
			if (objectListing == null) {
				objectListing = this.amazonS3.listObjects(listObjectsRequest);
			} else {
				objectListing = this.amazonS3.listNextBatchOfObjects(objectListing);
			}

			Set<Resource> newResources = getResourcesFromObjectSummaries(bucketName, objectListing.getObjectSummaries(), keyPattern);
			if (newResources.size() > 0) {
				resources.addAll(newResources);
			}

			for (String commonPrefix : objectListing.getCommonPrefixes()) {
				if (keyPathMatchesPartially(keyPattern, commonPrefix)) {
					findPathMatchingKeyInBucket(bucketName, resources, commonPrefix, keyPattern);
				}
			}
		} while (objectListing.isTruncated());
	}

	private String getRemainingPatternPart(String keyPattern, String path) {
		int numberOfSlashes = StringUtils.countOccurrencesOf(path, "/");
		int indexOfNthSlash = getIndexOfNthOccurence(keyPattern, "/", numberOfSlashes);
		if (indexOfNthSlash != -1) {
			return keyPattern.substring(indexOfNthSlash);
		} else {
			return null;
		}
	}

	private boolean keyPathMatchesPartially(String keyPattern, String keyPath) {
		int numberOfSlashes = StringUtils.countOccurrencesOf(keyPath, "/");
		int indexOfNthSlash = getIndexOfNthOccurence(keyPattern, "/", numberOfSlashes);
		if (indexOfNthSlash != -1) {
			return this.pathMatcher.match(keyPattern.substring(0, indexOfNthSlash), keyPath);
		} else {
			return false;
		}
	}

	private int getIndexOfNthOccurence(String str, String sub, int pos) {
		int result = 0;
		String subStr = str;
		for (int i = 0; i < pos; i++) {
			int nthOccurence = subStr.indexOf(sub);
			if (nthOccurence == -1) {
				return -1;
			} else {
				result += nthOccurence + 1;
				subStr = subStr.substring(nthOccurence + 1);
			}
		}
		return result;
	}

	private Set<Resource> getResourcesFromObjectSummaries(String bucketName, List<S3ObjectSummary> objectSummaries, String keyPattern) {
		Set<Resource> resources = new HashSet<Resource>();
		for (S3ObjectSummary objectSummary : objectSummaries) {
			String keyPath = S3_PROTOCOL_PREFIX + bucketName + "/" + objectSummary.getKey();
			if (this.pathMatcher.match(keyPattern, objectSummary.getKey())) {
				Resource resource = this.simpleStorageResourceLoader.getResource(keyPath);
				if (resource.exists()) {
					resources.add(resource);
				}
			}
		}

		return resources;
	}

	private List<String> findMatchingBuckets(String bucketPattern) {
		List<Bucket> buckets = this.amazonS3.listBuckets();
		List<String> matchingBuckets = new ArrayList<String>();
		for (Bucket bucket : buckets) {
			if (pathMatcher.match(bucketPattern, bucket.getName())) {
				matchingBuckets.add(bucket.getName());
			}
		}
		return matchingBuckets;
	}

	private String getBucketPattern(String locationPattern) {
		int prefixEnd = locationPattern.lastIndexOf(':') + 3;
		String locationWithoutPrefix = locationPattern.substring(prefixEnd);
		int bucketNameEnd = locationWithoutPrefix.indexOf('/');
		if (bucketNameEnd == -1) {
			throw new IllegalArgumentException("S3 pattern '" + locationPattern + "' is not a legal pattern. It must" +
					"at least contain on / to delimit the bucket and object");
		}
		return locationWithoutPrefix.substring(0, bucketNameEnd);
	}

	@Override
	public Resource getResource(String location) {
		return this.simpleStorageResourceLoader.getResource(location);
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.simpleStorageResourceLoader.getClassLoader();
	}
}
