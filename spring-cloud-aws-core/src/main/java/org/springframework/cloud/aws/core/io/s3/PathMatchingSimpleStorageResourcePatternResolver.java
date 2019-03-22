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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourcePatternResolver} implementation which allows an ant-style path
 * matching when loading S3 resources. Ant wildcards (*, ** and ?) are allowed in both,
 * bucket name and object name.
 * <p>
 * <b>WARNING:</b> Be aware that when you are using wildcards in the bucket name it can
 * take a very long time to parse all files. Moreover this implementation does not return
 * truncated results. This means that when handling huge buckets it could lead to serious
 * performance problems. For more information look at the
 * {@code findProgressivelyWithPartialMatch} method.
 * </p>
 *
 * @author Alain Sahli
 * @author Agim Emruli
 * @since 1.0
 */
public class PathMatchingSimpleStorageResourcePatternResolver
		implements ResourcePatternResolver {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(PathMatchingSimpleStorageResourcePatternResolver.class);

	private final AmazonS3 amazonS3;

	private final ResourcePatternResolver resourcePatternResolverDelegate;

	private PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * Construct a new instance of the
	 * {@link PathMatchingSimpleStorageResourcePatternResolver} with a
	 * {@link SimpleStorageProtocolResolver} to load AmazonS3 instances, and also a
	 * delegate {@link ResourcePatternResolver} to resolve resource on default path (like
	 * file and classpath).
	 * @param amazonS3 - used to retrieve the directory listings
	 * @param resourcePatternResolverDelegate - delegate resolver used to resolve common
	 * path (file, classpath, servlet etc.)
	 */
	public PathMatchingSimpleStorageResourcePatternResolver(AmazonS3 amazonS3,
			ResourcePatternResolver resourcePatternResolverDelegate) {
		Assert.notNull(amazonS3, "Amazon S3 must not be null");
		this.amazonS3 = AmazonS3ProxyFactory.createProxy(amazonS3);
		this.resourcePatternResolverDelegate = resourcePatternResolverDelegate;
	}

	/**
	 * Set the PathMatcher implementation to use for this resource pattern resolver.
	 * Default is AntPathMatcher.
	 * @param pathMatcher The pathMatches implementation used, must not be null
	 * @see AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		if (SimpleStorageNameUtils.isSimpleStorageResource(locationPattern)) {
			if (this.pathMatcher
					.isPattern(SimpleStorageNameUtils.stripProtocol(locationPattern))) {
				LOGGER.debug("Found wildcard pattern in location {}", locationPattern);
				return findPathMatchingResources(locationPattern);
			}
			else {
				return new Resource[] { this.resourcePatternResolverDelegate
						.getResource(locationPattern) };
			}
		}
		else {
			return this.resourcePatternResolverDelegate.getResources(locationPattern);
		}
	}

	protected Resource[] findPathMatchingResources(String locationPattern) {
		// Separate the bucket and key patterns as each one uses a different aws API for
		// resolving.
		String bucketPattern = SimpleStorageNameUtils
				.getBucketNameFromLocation(locationPattern);
		String keyPattern = SimpleStorageNameUtils
				.getObjectNameFromLocation(locationPattern);
		Set<Resource> resources;
		if (this.pathMatcher.isPattern(bucketPattern)) {
			List<String> matchingBuckets = findMatchingBuckets(bucketPattern);
			LOGGER.debug("Found wildcard in bucket name {} buckets found are {}",
					bucketPattern, matchingBuckets);

			// If the '**' wildcard is used in the bucket name, one have to inspect all
			// objects in the bucket. Therefore the keyPattern is prefixed with '**/' so
			// that the findPathMatchingKeys method knows that it must go through all
			// objects.
			if (bucketPattern.startsWith("**")) {
				keyPattern = "**/" + keyPattern;
			}
			resources = findPathMatchingKeys(keyPattern, matchingBuckets);
			LOGGER.debug("Found resources {} in buckets {}", resources, matchingBuckets);

		}
		else {
			LOGGER.debug("No wildcard in bucket name {} using single bucket name",
					bucketPattern);
			resources = findPathMatchingKeys(keyPattern,
					Collections.singletonList(bucketPattern));
		}

		return resources.toArray(new Resource[resources.size()]);
	}

	private Set<Resource> findPathMatchingKeys(String keyPattern,
			List<String> matchingBuckets) {
		Set<Resource> resources = new HashSet<>();
		if (this.pathMatcher.isPattern(keyPattern)) {
			for (String bucketName : matchingBuckets) {
				findPathMatchingKeyInBucket(bucketName, resources,
						getValidPrefix(keyPattern), keyPattern);
			}
		}
		else {
			for (String matchingBucket : matchingBuckets) {
				Resource resource = this.resourcePatternResolverDelegate
						.getResource(SimpleStorageNameUtils.getLocationForBucketAndObject(
								matchingBucket, keyPattern));
				if (resource.exists()) {
					resources.add(resource);
				}
			}
		}
		return resources;
	}

	private String getValidPrefix(String keyPattern) {
		int starIndex = keyPattern.indexOf("*");
		int markIndex = keyPattern.indexOf("?");
		int index = Math.min(starIndex == -1 ? keyPattern.length() : starIndex,
				markIndex == -1 ? keyPattern.length() : markIndex);
		String beforeIndex = keyPattern.substring(0, index);
		return beforeIndex.contains("/")
				? beforeIndex.substring(0, beforeIndex.lastIndexOf('/') + 1) : null;
	}

	private void findPathMatchingKeyInBucket(String bucketName, Set<Resource> resources,
			String prefix, String keyPattern) {
		String remainingPatternPart = getRemainingPatternPart(keyPattern, prefix);
		if (remainingPatternPart != null && remainingPatternPart.startsWith("**")) {
			findAllResourcesThatMatches(bucketName, resources, prefix, keyPattern);
		}
		else {
			findProgressivelyWithPartialMatch(bucketName, resources, prefix, keyPattern);
		}
	}

	private void findAllResourcesThatMatches(String bucketName, Set<Resource> resources,
			String prefix, String keyPattern) {
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
				.withBucketName(bucketName).withPrefix(prefix);
		ObjectListing objectListing = null;

		do {
			try {
				if (objectListing == null) {
					objectListing = this.amazonS3.listObjects(listObjectsRequest);
				}
				else {
					objectListing = this.amazonS3.listNextBatchOfObjects(objectListing);
				}
				Set<Resource> newResources = getResourcesFromObjectSummaries(bucketName,
						keyPattern, objectListing.getObjectSummaries());
				if (!newResources.isEmpty()) {
					resources.addAll(newResources);
				}
			}
			catch (AmazonS3Exception e) {
				if (301 != e.getStatusCode()) {
					throw e;
				}
			}
		}
		while (objectListing != null && objectListing.isTruncated());
	}

	/**
	 * Searches for matching keys progressively. This means that instead of retrieving all
	 * keys given a prefix, it goes down one level at a time and filters out all
	 * non-matching results. This avoids a lot of unused requests results. WARNING: This
	 * method does not truncate results. Therefore all matching resources will be returned
	 * regardless of the truncation.
	 * @param bucketName name of the bucket
	 * @param resources retrieved resources
	 * @param prefix bucket prefix
	 * @param keyPattern pattern for key
	 */
	private void findProgressivelyWithPartialMatch(String bucketName,
			Set<Resource> resources, String prefix, String keyPattern) {
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
				.withBucketName(bucketName).withDelimiter("/").withPrefix(prefix);
		ObjectListing objectListing = null;

		do {
			if (objectListing == null) {
				objectListing = this.amazonS3.listObjects(listObjectsRequest);
			}
			else {
				objectListing = this.amazonS3.listNextBatchOfObjects(objectListing);
			}

			Set<Resource> newResources = getResourcesFromObjectSummaries(bucketName,
					keyPattern, objectListing.getObjectSummaries());
			if (!newResources.isEmpty()) {
				resources.addAll(newResources);
			}

			for (String commonPrefix : objectListing.getCommonPrefixes()) {
				if (isKeyPathMatchesPartially(keyPattern, commonPrefix)) {
					findPathMatchingKeyInBucket(bucketName, resources, commonPrefix,
							keyPattern);
				}
			}
		}
		while (objectListing.isTruncated());
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
			return this.pathMatcher.match(keyPattern.substring(0, indexOfNthSlash),
					keyPath);
		}
		else {
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
			}
			else {
				result += nthOccurrence + 1;
				subStr = subStr.substring(nthOccurrence + 1);
			}
		}
		return result;
	}

	private Set<Resource> getResourcesFromObjectSummaries(String bucketName,
			String keyPattern, List<S3ObjectSummary> objectSummaries) {
		Set<Resource> resources = new HashSet<>();
		for (S3ObjectSummary objectSummary : objectSummaries) {
			String keyPath = SimpleStorageNameUtils
					.getLocationForBucketAndObject(bucketName, objectSummary.getKey());
			if (this.pathMatcher.match(keyPattern, objectSummary.getKey())) {
				Resource resource = this.resourcePatternResolverDelegate
						.getResource(keyPath);
				resources.add(resource);
			}
		}

		return resources;
	}

	private List<String> findMatchingBuckets(String bucketPattern) {
		List<Bucket> buckets = this.amazonS3.listBuckets();
		List<String> matchingBuckets = new ArrayList<>();
		for (Bucket bucket : buckets) {
			this.amazonS3.getBucketLocation(bucket.getName());
			if (this.pathMatcher.match(bucketPattern, bucket.getName())) {
				matchingBuckets.add(bucket.getName());
			}
		}
		return matchingBuckets;
	}

	@Override
	public Resource getResource(String location) {
		return this.resourcePatternResolverDelegate.getResource(location);
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.resourcePatternResolverDelegate.getClassLoader();
	}

}
