/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author Agim Emruli
 * @since 1.0
 */
public class PathMatchingSimpleStorageResourcePatternResolver implements ResourcePatternResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(PathMatchingSimpleStorageResourcePatternResolver.class);
	private final AmazonS3 amazonS3;
	private final ResourceLoader simpleStorageResourceLoader;
	private final ResourcePatternResolver resourcePatternResolverDelegate;
	private PathMatcher pathMatcher = new AntPathMatcher();


	public PathMatchingSimpleStorageResourcePatternResolver(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
		this.simpleStorageResourceLoader = new SimpleStorageResourceLoader(amazonS3);
		this.resourcePatternResolverDelegate = new PathMatchingResourcePatternResolver();
	}

	public PathMatchingSimpleStorageResourcePatternResolver(AmazonS3 amazonS3, ClassLoader classLoader) {
		this.amazonS3 = amazonS3;
		this.simpleStorageResourceLoader = new SimpleStorageResourceLoader(amazonS3, classLoader);
		this.resourcePatternResolverDelegate = new PathMatchingResourcePatternResolver(classLoader);
	}

	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		if (SimpleStorageNameUtils.isSimpleStorageResource(locationPattern)) {
			if (this.pathMatcher.isPattern(SimpleStorageNameUtils.stripProtocol(locationPattern))) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Found wildcard pattern in location {}", locationPattern);
				}
				return findPathMatchingResources(locationPattern);
			} else {
				return new Resource[]{this.simpleStorageResourceLoader.getResource(locationPattern)};
			}
		} else {
			return this.resourcePatternResolverDelegate.getResources(locationPattern);
		}
	}

	protected Resource[] findPathMatchingResources(String locationPattern) {
		String bucketPattern = SimpleStorageNameUtils.getBucketNameFromLocation(locationPattern);
		String keyPattern = SimpleStorageNameUtils.getObjectNameFromLocation(locationPattern);
		Set<Resource> resources;
		if (this.pathMatcher.isPattern(bucketPattern)) {
			List<String> matchingBuckets = findMatchingBuckets(bucketPattern);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Found wildcard in bucket name {} buckets found are {}", bucketPattern, matchingBuckets);
			}

			if (bucketPattern.startsWith("**")) {
				keyPattern = "**/" + keyPattern;
			}
			resources = findPathMatchingKeys(keyPattern, matchingBuckets);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Found resources {} in buckets {}", resources, matchingBuckets);
			}

		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("No wildcard in bucket name {} using single bucket name", bucketPattern);
			}
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
				Resource resource = this.simpleStorageResourceLoader.getResource(SimpleStorageNameUtils.getLocationForBucketAndObject(matchingBucket, keyPattern));
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
			findProgressivelyWithPartialMatch(bucketName, resources, prefix, keyPattern);
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

			Set<Resource> newResources = getResourcesFromObjectSummaries(bucketName, keyPattern, objectListing.getObjectSummaries());
			if (!newResources.isEmpty()) {
				resources.addAll(newResources);
			}
		} while (objectListing.isTruncated());
	}

	private void findProgressivelyWithPartialMatch(String bucketName, Set<Resource> resources, String prefix, String keyPattern) {
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withDelimiter("/").withPrefix(prefix);
		ObjectListing objectListing = null;

		do {
			if (objectListing == null) {
				objectListing = this.amazonS3.listObjects(listObjectsRequest);
			} else {
				objectListing = this.amazonS3.listNextBatchOfObjects(objectListing);
			}

			Set<Resource> newResources = getResourcesFromObjectSummaries(bucketName, keyPattern, objectListing.getObjectSummaries());
			if (!newResources.isEmpty()) {
				resources.addAll(newResources);
			}

			for (String commonPrefix : objectListing.getCommonPrefixes()) {
				if (isKeyPathMatchesPartially(keyPattern, commonPrefix)) {
					findPathMatchingKeyInBucket(bucketName, resources, commonPrefix, keyPattern);
				}
			}
		} while (objectListing.isTruncated());
	}

	private String getRemainingPatternPart(String keyPattern, String path) {
		int numberOfSlashes = StringUtils.countOccurrencesOf(path, "/");
		int indexOfNthSlash = getIndexOfNthOccurrence(keyPattern, "/", numberOfSlashes);
		return indexOfNthSlash == -1 ? null : keyPattern.substring(indexOfNthSlash);
	}

	private boolean isKeyPathMatchesPartially(String keyPattern, String keyPath) {
		int numberOfSlashes = StringUtils.countOccurrencesOf(keyPath, "/");
		int indexOfNthSlash = getIndexOfNthOccurrence(keyPattern, "/", numberOfSlashes);
		if (indexOfNthSlash != -1) {
			return this.pathMatcher.match(keyPattern.substring(0, indexOfNthSlash), keyPath);
		} else {
			return false;
		}
	}

	private int getIndexOfNthOccurrence(String str, String sub, int pos) {
		int result = 0;
		String subStr = str;
		for (int i = 0; i < pos; i++) {
			int nthOccurrence = subStr.indexOf(sub);
			if (nthOccurrence == -1) {
				return -1;
			} else {
				result += nthOccurrence + 1;
				subStr = subStr.substring(nthOccurrence + 1);
			}
		}
		return result;
	}

	private Set<Resource> getResourcesFromObjectSummaries(String bucketName, String keyPattern, List<S3ObjectSummary> objectSummaries) {
		Set<Resource> resources = new HashSet<Resource>();
		for (S3ObjectSummary objectSummary : objectSummaries) {
			String keyPath = SimpleStorageNameUtils.getLocationForBucketAndObject(bucketName, objectSummary.getKey());
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
			if (this.pathMatcher.match(bucketPattern, bucket.getName())) {
				matchingBuckets.add(bucket.getName());
			}
		}
		return matchingBuckets;
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
