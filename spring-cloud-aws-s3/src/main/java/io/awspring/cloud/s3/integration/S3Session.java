/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.s3.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.IoUtils;

/**
 * An Amazon S3 {@link Session} implementation.
 *
 * @author Artem Bilan
 * @author Jim Krygowski
 * @author Anwar Chirakkattil
 * @author Xavier François
 * @author Rogerio Lino
 *
 * @since 4.0
 */
public class S3Session implements Session<S3Object> {

	private final S3Client amazonS3;

	private String endpoint;

	public S3Session(S3Client amazonS3) {
		Assert.notNull(amazonS3, "'amazonS3' must not be null.");
		this.amazonS3 = amazonS3;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public S3Object[] list(String path) {
		String[] bucketPrefix = splitPathToBucketAndKey(path, false);

		ListObjectsRequest.Builder listObjectsRequest = ListObjectsRequest.builder().bucket(bucketPrefix[0]);
		if (bucketPrefix.length > 1) {
			listObjectsRequest.prefix(bucketPrefix[1]);
		}

		/*
		 * For listing objects, Amazon S3 returns up to 1,000 keys in the response. If you have more than 1,000 keys in
		 * your bucket, the response will be truncated. You should always check for if the response is truncated.
		 */
		ListObjectsResponse objectListing;
		List<S3Object> objectSummaries = new ArrayList<>();
		do {
			objectListing = this.amazonS3.listObjects(listObjectsRequest.build());
			List<S3Object> contents = objectListing.contents();
			objectSummaries.addAll(contents);
			if (Boolean.TRUE.equals(objectListing.isTruncated())) {
				listObjectsRequest.marker(contents.get(contents.size() - 1).key());
			}
		}
		while (Boolean.TRUE.equals(objectListing.isTruncated()));

		return objectSummaries.toArray(new S3Object[0]);
	}

	@Override
	public String[] listNames(String path) {
		String[] bucketPrefix = splitPathToBucketAndKey(path, false);

		ListObjectsRequest.Builder listObjectsRequest = ListObjectsRequest.builder().bucket(bucketPrefix[0]);
		if (bucketPrefix.length > 1) {
			listObjectsRequest.prefix(bucketPrefix[1]);
		}

		/*
		 * For listing objects, Amazon S3 returns up to 1,000 keys in the response. If you have more than 1,000 keys in
		 * your bucket, the response will be truncated. You should always check for if the response is truncated.
		 */
		ListObjectsResponse objectListing;
		List<String> names = new ArrayList<>();
		do {
			objectListing = this.amazonS3.listObjects(listObjectsRequest.build());
			List<S3Object> contents = objectListing.contents();
			for (S3Object objectSummary : contents) {
				names.add(objectSummary.key());
			}
			if (Boolean.TRUE.equals(objectListing.isTruncated())) {
				listObjectsRequest.marker(contents.get(contents.size() - 1).key());
			}
		}
		while (Boolean.TRUE.equals(objectListing.isTruncated()));

		return names.toArray(new String[0]);
	}

	@Override
	public boolean remove(String path) {
		String[] bucketKey = splitPathToBucketAndKey(path, true);
		this.amazonS3.deleteObject(request -> request.bucket(bucketKey[0]).key(bucketKey[1]));
		return true;
	}

	@Override
	public void rename(String pathFrom, String pathTo) {
		String[] bucketKeyFrom = splitPathToBucketAndKey(pathFrom, true);
		String[] bucketKeyTo = splitPathToBucketAndKey(pathTo, true);
		CopyObjectRequest.Builder copyRequest = CopyObjectRequest.builder().sourceBucket(bucketKeyFrom[0])
				.sourceKey(bucketKeyFrom[1]).destinationBucket(bucketKeyTo[0]).destinationKey(bucketKeyTo[1]);
		this.amazonS3.copyObject(copyRequest.build());

		// Delete the source
		this.amazonS3.deleteObject(request -> request.bucket(bucketKeyFrom[0]).key(bucketKeyFrom[1]));
	}

	@Override
	public void read(String source, OutputStream outputStream) throws IOException {
		String[] bucketKey = splitPathToBucketAndKey(source, true);
		GetObjectRequest.Builder getObjectRequest = GetObjectRequest.builder().bucket(bucketKey[0]).key(bucketKey[1]);
		try (InputStream inputStream = this.amazonS3.getObject(getObjectRequest.build())) {
			StreamUtils.copy(inputStream, outputStream);
		}
	}

	@Override
	public void write(InputStream inputStream, String destination) {
		Assert.notNull(inputStream, "'inputStream' must not be null.");
		String[] bucketKey = splitPathToBucketAndKey(destination, true);
		PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder().bucket(bucketKey[0]).key(bucketKey[1]);
		try {
			this.amazonS3.putObject(putObjectRequest.build(), RequestBody.fromBytes(IoUtils.toByteArray(inputStream)));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public void append(InputStream inputStream, String destination) {
		throw new UnsupportedOperationException("The 'append' operation isn't supported by the Amazon S3 protocol.");
	}

	@Override
	public boolean mkdir(String directory) {
		this.amazonS3.createBucket(request -> request.bucket(directory));
		return true;
	}

	@Override
	public boolean rmdir(String directory) {
		this.amazonS3.deleteBucket(request -> request.bucket(directory));
		return true;
	}

	@Override
	public boolean exists(String path) {
		String[] bucketKey = splitPathToBucketAndKey(path, true);
		try {
			this.amazonS3.getObjectAttributes(request -> request.bucket(bucketKey[0]).key(bucketKey[1]));
		}
		catch (NoSuchKeyException ex) {
			return false;
		}
		return true;
	}

	@Override
	public InputStream readRaw(String source) {
		String[] bucketKey = splitPathToBucketAndKey(source, true);
		return this.amazonS3.getObject(request -> request.bucket(bucketKey[0]).key(bucketKey[1]));
	}

	@Override
	public void close() {
		// No-op. This session is just direct wrapper for the AmazonS3
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean finalizeRaw() {
		return true;
	}

	@Override
	public Object getClientInstance() {
		return this.amazonS3;
	}

	@Override
	public String getHostPort() {
		if (this.endpoint != null) {
			return this.endpoint;
		}
		else {
			synchronized (this) {
				if (this.endpoint != null) {
					return this.endpoint;
				}
				DirectFieldAccessor dfa = new DirectFieldAccessor(this.amazonS3.utilities());
				Region region = (Region) dfa.getPropertyValue("region");
				this.endpoint = String.format("%s.%s:%d", S3Client.SERVICE_NAME, region, 443);
				return this.endpoint;
			}
		}
	}

	public String normalizeBucketName(String path) {
		return splitPathToBucketAndKey(path, false)[0];
	}

	private String[] splitPathToBucketAndKey(String path, boolean requireKey) {
		Assert.hasText(path, "'path' must not be empty String.");

		path = StringUtils.trimLeadingCharacter(path, '/');

		String[] bucketKey = path.split("/", 2);

		if (requireKey) {
			Assert.state(bucketKey.length == 2, "'path' must in pattern [BUCKET/KEY].");
			Assert.state(bucketKey[0].length() >= 3, "S3 bucket name must be at least 3 characters long.");
		}
		else {
			Assert.state(bucketKey.length > 0 && bucketKey[0].length() >= 3,
					"S3 bucket name must be at least 3 characters long.");
		}
		return bucketKey;
	}

}
