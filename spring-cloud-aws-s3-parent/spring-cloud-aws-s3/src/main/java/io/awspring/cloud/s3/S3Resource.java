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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import org.springframework.core.io.AbstractResource;

/**
 * {@link org.springframework.core.io.Resource} implementation for S3 objects.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class S3Resource extends AbstractResource {

	private final Location location;

	private final S3Client s3Client;

	private ObjectMetadata metadata;

	public static S3Resource create(String location, S3Client s3Client) {
		if (Location.isSimpleStorageResource(location)) {
			return new S3Resource(location, s3Client);
		}
		return null;
	}

	public S3Resource(String location, S3Client s3Client) {
		this(Location.of(location), s3Client);
	}

	public S3Resource(String bucket, String key, S3Client s3Client) {
		this(new Location(bucket, key, null), s3Client);
	}

	public S3Resource(Location location, S3Client s3Client) {
		this.location = location;
		this.s3Client = s3Client;
	}

	@Override
	public URL getURL() throws IOException {
		String encodedObjectName = URLEncoder.encode(location.getObject(), StandardCharsets.UTF_8.toString());
		return new URL("https", location.getBucket() + ".s3.amazonaws.com", "/" + encodedObjectName);
	}

	@Override
	public String getDescription() {
		return location.toString();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return s3Client.getObject(request -> request.bucket(location.getBucket()).key(location.getObject())
				.versionId(location.getVersion()));
	}

	@Override
	public boolean exists() {
		try {
			fetchMetadata();
			return true;
		}
		catch (NoSuchKeyException e) {
			return false;
		}
	}

	@Override
	public long contentLength() throws IOException {
		if (metadata == null) {
			fetchMetadata();
		}
		return metadata.contentLength;
	}

	@Override
	public long lastModified() throws IOException {
		if (metadata == null) {
			fetchMetadata();
		}
		return metadata.lastModified.toEpochMilli();
	}

	@Override
	public File getFile() throws IOException {
		throw new UnsupportedOperationException("Amazon S3 resource can not be resolved to java.io.File objects.Use "
				+ "getInputStream() to retrieve the contents of the object!");
	}

	private void fetchMetadata() {
		HeadObjectResponse response = s3Client.headObject(request -> request.bucket(location.getBucket())
				.key(location.getObject()).versionId(location.getVersion()));
		this.metadata = new ObjectMetadata(response);
	}

	private static class ObjectMetadata {

		private final Long contentLength;

		private final Instant lastModified;

		ObjectMetadata(HeadObjectResponse headObjectResponse) {
			this.contentLength = headObjectResponse.contentLength();
			this.lastModified = headObjectResponse.lastModified();
		}

	}

}
