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

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

/**
 * Higher level abstraction over {@link S3Client} providing methods for the most common use cases. Implemented by
 * {@link S3Template}.
 *
 * @author Maciej Walkowiak
 * @author Ziemowit Stolarczyk
 * @since 3.0
 */
public interface S3Operations {

	/**
	 * Creates a bucket in S3.
	 *
	 * @param bucketName - the bucket name
	 * @return created bucket location {@link CreateBucketResponse#location()}
	 */
	String createBucket(String bucketName);

	/**
	 * Deletes a S3 bucket.
	 *
	 * @param bucketName - the bucket name
	 */
	void deleteBucket(String bucketName);

	/**
	 * Checks if an S3 bucket exists.
	 *
	 * @param bucketName - the bucket name
	 * @return true if bucket exists; false otherwise
	 */
	boolean bucketExists(String bucketName);

	/**
	 * Deletes an object from S3 bucket.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 */
	void deleteObject(String bucketName, String key);

	/**
	 * Deletes an object from S3 bucket.
	 *
	 * @param s3Url - the S3 url s3://bucket/key
	 */
	void deleteObject(String s3Url);

	/**
	 * Checks if an S3 object exists.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @return true if object exists; false otherwise
	 */
	boolean objectExists(String bucketName, String key);

	/**
	 * Returns some or all (up to 1,000) of the objects in a bucket. Does not handle pagination. If you need pagination
	 * you should use {@link S3PathMatchingResourcePatternResolver} or {@link S3Client}
	 *
	 * @param bucketName - the bucket name
	 * @return list of {@link S3Resource}
	 */
	List<S3Resource> listAllObjects(String bucketName);

	/**
	 * Returns some or all (up to 1,000) of the objects in a bucket. Does not handle pagination. If you need pagination
	 * you should use {@link S3PathMatchingResourcePatternResolver} or {@link S3Client}
	 *
	 * @param bucketName - the bucket name
	 * @param prefix - objects prefix
	 * @return list of {@link S3Resource}
	 */
	List<S3Resource> listObjects(String bucketName, String prefix);

	/**
	 * Stores a Java object in a S3 bucket. Uses {@link S3ObjectConverter} for serialization.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @param object - the Java object to serialize and store
	 * @return created {@link S3Resource}.
	 */
	S3Resource store(String bucketName, String key, Object object);

	/**
	 * Creates an {@link S3Resource} for given bucket name and object key using {@link S3OutputStreamProvider}
	 * configured on the implementation class ({@link S3Template}).
	 * <p>
	 * Note that calling this method does not create an actual object on S3.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @return the {@link S3Resource}
	 */
	S3Resource createResource(String bucketName, String key);

	/**
	 * Reads a Java object from a S3 bucket. Uses {@link S3ObjectConverter} for deserialization.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @param clazz - the class of the read object
	 * @param <T> - the type of the read object
	 * @return an object
	 */
	<T> T read(String bucketName, String key, Class<T> clazz);

	/**
	 * Uploads data from an input stream to a S3 bucket.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @param inputStream - the input stream
	 * @param objectMetadata - the object metadata
	 * @return created {@link S3Resource}
	 */
	S3Resource upload(String bucketName, String key, InputStream inputStream, @Nullable ObjectMetadata objectMetadata);

	/**
	 * Uploads data from an input stream to a S3 bucket.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @param inputStream - the input stream
	 * @return created {@link S3Resource}
	 */
	default S3Resource upload(String bucketName, String key, InputStream inputStream) {
		return upload(bucketName, key, inputStream, null);
	}

	/**
	 * Downloads object from S3.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @return downloaded object represented as {@link S3Resource}
	 */
	S3Resource download(String bucketName, String key);

	/**
	 * Creates a signed URL for retrieving an object from S3.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @param duration - duration that the URL will work
	 * @return a {@link URL} representing the signed URL
	 */
	URL createSignedGetURL(String bucketName, String key, Duration duration);

	/**
	 * Creates a signed URL for putting an object into S3.
	 *
	 * @param bucketName - the bucket name
	 * @param key - the object key
	 * @param duration - duration that the URL will work
	 * @return a {@link URL} representing the signed URL
	 */
	URL createSignedPutURL(String bucketName, String key, Duration duration, @Nullable ObjectMetadata metadata,
			@Nullable String contentType);

	default URL createSignedPutURL(String bucketName, String key, Duration duration) {
		return createSignedPutURL(bucketName, key, duration, null, null);
	}
}
