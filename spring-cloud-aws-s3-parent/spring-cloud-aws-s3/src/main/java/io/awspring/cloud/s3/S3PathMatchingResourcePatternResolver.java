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
package io.awspring.cloud.s3;

import static io.awspring.cloud.s3.Location.PATH_DELIMITER;
import static io.awspring.cloud.s3.Location.S3_PROTOCOL_PREFIX;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * A {@link ResourcePatternResolver} implementation which allows an ant-style path matching when loading S3 resources.
 * Ant wildcards (*, ** and ?) are allowed in both, bucket name and object name.
 * <p>
 * <b>WARNING:</b> Be aware that when you are using wildcards in the bucket name it can take a very long time to parse
 * all files. Moreover this implementation does not return truncated results. This means that when handling huge buckets
 * it could lead to serious performance problems. For more information look at the
 * {@code findResourcesInBucketWithKeyPattern} method.
 * </p>
 *
 * @author Tobias Soloschenko
 * @since 2.0
 */
public class S3PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(S3PathMatchingResourcePatternResolver.class);

	private final S3Client s3Client;

	private final ResourcePatternResolver resourcePatternResolverDelegate;

	private final PathMatcher pathMatcher;

	private List<String> wildCardSeparators;

	private final S3OutputStreamProvider s3OutputStreamProvider;

	/**
	 * Creates a {@link S3PathMatchingResourcePatternResolver} with the given s3Client and
	 * resourcePatternResolverDelegate. For the S3 Resource a {@link InMemoryBufferingS3OutputStreamProvider} is used in
	 * case the resource should be modified and for the pattern matching a {@link AntPathMatcher} is used.
	 *
	 * @param s3Client                        the s3Client of the Amazon SDK
	 * @param resourcePatternResolverDelegate the resourcePatternResolverDelegate which is used if the given scheme is
	 *                                        not S3. In this case all processing is delegated to this implementation.
	 */
	public S3PathMatchingResourcePatternResolver(S3Client s3Client,
												 ResourcePatternResolver resourcePatternResolverDelegate) {
		this(s3Client, resourcePatternResolverDelegate,
			new InMemoryBufferingS3OutputStreamProvider(s3Client, new PropertiesS3ObjectContentTypeResolver()),
			new AntPathMatcher(), List.of("**", "*", "?"));
	}

	/**
	 * Creates a {@link S3PathMatchingResourcePatternResolver} with the given s3Client and
	 * resourcePatternResolverDelegate.
	 *
	 * @param s3Client                        the s3Client of the Amazon SDK
	 * @param resourcePatternResolverDelegate the resourcePatternResolverDelegate which is used if the given scheme is
	 *                                        not S3. In this case all processing is delegated to this implementation.
	 * @param s3OutputStreamProvider          The s3OutputStreamProvider used if the resource is going to be written
	 * @param pathMatcher                     used to resolve resources and bucket names
	 * @param wildCardSeparators              the wildcard separators to determine the prefix. You can also use
	 *                                        Collections.emptyList(), but this will cause performance and request impacts.
	 */
	public S3PathMatchingResourcePatternResolver(S3Client s3Client,
												 ResourcePatternResolver resourcePatternResolverDelegate, S3OutputStreamProvider s3OutputStreamProvider,
												 PathMatcher pathMatcher, List<String> wildCardSeparators) {
		Assert.notNull(s3Client, "S3Client must not be null");
		Assert.notNull(resourcePatternResolverDelegate, "ResourcePatternResolver must not be null");
		Assert.notNull(s3OutputStreamProvider, "S3OutputStreamProvider must not be null");
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		Assert.notNull(wildCardSeparators, "WildCardSeparators must not be null");
		this.s3Client = s3Client;
		this.resourcePatternResolverDelegate = resourcePatternResolverDelegate;
		this.s3OutputStreamProvider = s3OutputStreamProvider;
		this.pathMatcher = pathMatcher;
		this.wildCardSeparators = wildCardSeparators;
	}

	/**
	 * Gets all resources based on the given location pattern. If the location pattern is not using the s3 scheme the
	 * operation is delegated to resourcePatternResolverDelegate.
	 *
	 * @param locationPattern the location pattern to get all resources from
	 * @return an array with all resources
	 * @throws IOException if something went wrong during the resource resolving
	 */
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		LOGGER.debug("Get resources of the following location pattern {}", locationPattern);
		Resource[] resources = locationPattern.toLowerCase().startsWith(S3_PROTOCOL_PREFIX)
			? findResourcesInBucketsWithPatterns(locationPattern)
			: this.resourcePatternResolverDelegate.getResources(locationPattern);
		String resourceUrisString = Arrays.stream(resources).map(this::getUriAsString).collect(Collectors.joining(","));
		LOGGER.debug("Found the following resources: {}", resourceUrisString);
		return resources;
	}

	/**
	 * Gets a single resource. Note: This method does not accept any kind of patterns.
	 *
	 * @param location the location which points to a resource
	 * @return the resource
	 */
	@Override
	public Resource getResource(String location) {
		LOGGER.debug("Get resource of the following location {}", location);
		Resource resource = location.toLowerCase().startsWith(S3_PROTOCOL_PREFIX) ? createS3Resource(location)
			: this.resourcePatternResolverDelegate.getResource(location);
		LOGGER.debug("Found the following resource: {}", getUriAsString(resource));
		return resource;
	}

	/**
	 * Gets the classloader. Delegates to {@link this.resourcePatternResolverDelegate#getClassLoader()}.
	 *
	 * @return the classloader
	 */
	@Override
	public ClassLoader getClassLoader() {
		return this.resourcePatternResolverDelegate.getClassLoader();
	}

	/**
	 * Finds all resources in all buckets with the given location pattern.
	 *
	 * @param locationPattern the location pattern providing a pattern for the bucket and the resource
	 * @return an array with all resources found in all resolved s3 buckets
	 */
	private Resource[] findResourcesInBucketsWithPatterns(String locationPattern) {
		String s3BucketNamePattern = getS3BucketNamePattern(locationPattern);
		LOGGER.debug("The s3 bucket name pattern is {}", s3BucketNamePattern);
		String s3KeyPattern = substringAfter(locationPattern, s3BucketNamePattern + "/");
		LOGGER.debug("The s3 key pattern is {}", s3KeyPattern);
		return (pathMatcher.isPattern(s3BucketNamePattern) ? findMatchingBuckets(s3BucketNamePattern)
			: List.of(s3BucketNamePattern)).stream().flatMap(
				s3BucketName -> findResourcesInBucketWithKeyPattern(s3BucketName, s3KeyPattern).stream())
			.toArray(Resource[]::new);
	}

	/**
	 * Finds all resources in the given bucket with the given key pattern.
	 *
	 * @param s3BucketName the bucket name to search for resources with the given key pattern
	 * @param s3KeyPattern the key pattern to search for
	 * @return a list of resources found in the given s3 bucket
	 */
	private List<Resource> findResourcesInBucketWithKeyPattern(String s3BucketName, String s3KeyPattern) {
		ListObjectsV2Request listObjectsV2Request = getListObjectsV2RequestBuilder(s3BucketName, s3KeyPattern).build();
		LOGGER.debug("Listing objects from bucket {} with prefix: {}", listObjectsV2Request.bucket(),
			listObjectsV2Request.prefix());
		ListObjectsV2Iterable listObjectsV2Iterable = s3Client.listObjectsV2Paginator(listObjectsV2Request);
		return listObjectsV2Iterable.stream().flatMap(listObjectsV2Response -> {
				LOGGER.debug("List of s3 objects: {}", listObjectsV2Response.contents().size());
				return listObjectsV2Response.contents().stream();
			}).filter(s3Object -> {
				boolean s3ObjectKeyMatchesS3KeyPattern = pathMatcher.match(s3KeyPattern, s3Object.key());
				LOGGER.debug("The s3 object key ({}) matches the s3 key pattern ({}): {}", s3Object.key(), s3KeyPattern,
					s3ObjectKeyMatchesS3KeyPattern);
				return s3ObjectKeyMatchesS3KeyPattern;
			}).peek(s3Object -> LOGGER.debug("Resolved key: {} based on pattern: {}", s3Object.key(), s3KeyPattern))
			.map(s3Object -> getResource(S3_PROTOCOL_PREFIX + s3BucketName + "/" + s3Object.key()))
			.collect(Collectors.toList());
	}

	/**
	 * Gets the list objects request builder which might be initialized with a prefix depending on the wild card in the
	 * key pattern.
	 *
	 * @param s3BucketName the s3 bucket name
	 * @param s3KeyPattern the s3 key pattern
	 * @return the ListObjectsV2Request.Builder to perform the objects listing with
	 */
	private ListObjectsV2Request.Builder getListObjectsV2RequestBuilder(String s3BucketName, String s3KeyPattern) {
		// To improve performance we list only files to until the optionalPrefix path delimiter before the
		// optionalPrefix wild card
		Optional<String> optionalPrefix = wildCardSeparators.stream().filter(s3KeyPattern::contains)
			.map(wildcard -> substringBefore(s3KeyPattern, wildcard))
			.map(s3KeyPatternSubStringBeforeWildCard -> substringBeforeLast(s3KeyPatternSubStringBeforeWildCard,
				PATH_DELIMITER))
			.findFirst();
		ListObjectsV2Request.Builder listObjectsV2RequestBuilder = ListObjectsV2Request.builder().bucket(s3BucketName);
		if (optionalPrefix.isPresent()) {
			listObjectsV2RequestBuilder = listObjectsV2RequestBuilder.prefix(optionalPrefix.get());
		}
		return listObjectsV2RequestBuilder;
	}

	/**
	 * Creates a S3 resource based on the given location.
	 *
	 * @param location the location to create the S3 resource from
	 * @return the created S3 resource
	 */
	protected S3Resource createS3Resource(String location) {
		LOGGER.debug("Creating resource based on location: {}", location);
		return Optional.ofNullable(S3Resource.create(location, s3Client, s3OutputStreamProvider))
			.orElseThrow(() -> new IllegalStateException(
				"The s3 resource based on the location: " + location + " was not created correctly"));
	}

	/**
	 * Gets the s3 bucket name pattern.
	 *
	 * @param locationPattern the location pattern to extract the s3 bucket name pattern from
	 * @return the s3 bucket name pattern
	 */
	public String getS3BucketNamePattern(String locationPattern) {
		String locationPatternWithoutS3Scheme = locationPattern.substring(S3_PROTOCOL_PREFIX.length());
		return locationPatternWithoutS3Scheme.substring(0, locationPatternWithoutS3Scheme.indexOf('/'));
	}

	/**
	 * Finds all matching buckets which matches the given pattern.
	 *
	 * @param bucketPattern bucket pattern to check for
	 * @return list of bucket names
	 */
	private List<String> findMatchingBuckets(String bucketPattern) {
		return this.s3Client.listBuckets().buckets().stream().map(Bucket::name).filter(name -> {
				boolean bucketNameMatchesBucketPattern = this.pathMatcher.match(bucketPattern, name);
				LOGGER.debug("The s3 bucket name ({}) matches the s3 bucket pattern ({}): {}", name, bucketPattern,
					bucketNameMatchesBucketPattern);
				return bucketNameMatchesBucketPattern;
			}).peek(name -> LOGGER.debug("Resolved bucket name: {} based on pattern: {}", name, bucketPattern))
			.collect(Collectors.toList());
	}

	/**
	 * Gets the String representation of the given Resource URI.
	 *
	 * @param resource the Resource of which to retrieve the URI to be converted into a String
	 * @return the String representation of the URI of the resource
	 */
	private String getUriAsString(Resource resource) {
		try {
			return resource.getURI().toASCIIString();
		} catch (IOException e) {
			LOGGER.warn("The URI of the resource couldn't be retrieved", e);
			return "<resource_uri>";
		}
	}

	/**
	 * Gets the substring before the last occurrence of a separator. The separator is not returned.
	 *
	 * @param str       the String to get a substring from, may be null
	 * @param separator the String to search for, may be null
	 * @return the substring after the first occurrence of the separator, {@code null} if null String input
	 */
	public String substringAfter(final String str, final String separator) {
		if (str == null || separator == null) {
			return null;
		}
		if (str.isEmpty() || separator.isEmpty()) {
			return "";
		}
		final int pos = str.indexOf(separator);
		return pos == -1 ? "" : str.substring(pos + separator.length());
	}

	/**
	 * Gets the substring before the first occurrence of a separator. The separator is not returned.
	 *
	 * @param str       the String to get a substring from, may be null
	 * @param separator the String to search for, may be null
	 * @return the substring before the first occurrence of the separator, {@code null} if null String input
	 */
	public String substringBefore(final String str, final String separator) {
		if (str == null || separator == null) {
			return null;
		}
		if (str.isEmpty() || separator.isEmpty()) {
			return "";
		}
		final int pos = str.indexOf(separator);
		return pos == -1 ? str : str.substring(0, pos);
	}

	/**
	 * Gets the substring before the last occurrence of a separator. The separator is not returned.
	 *
	 * @param str       the String to get a substring from, may be null
	 * @param separator the String to search for, may be null
	 * @return the substring before the last occurrence of the separator, {@code null} if null String input
	 */
	public String substringBeforeLast(final String str, final String separator) {
		if (str == null || separator == null) {
			return null;
		}
		if (str.isEmpty() || separator.isEmpty()) {
			return "";
		}
		final int pos = str.lastIndexOf(separator);
		return pos == -1 ? str : str.substring(0, pos);
	}
}
