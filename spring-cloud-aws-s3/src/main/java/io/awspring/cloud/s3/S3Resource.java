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
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * {@link org.springframework.core.io.Resource} implementation for S3 objects.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @author Yuki Yoshida
 * @author Sharang Gupta
 * @since 3.0
 */
public class S3Resource extends AbstractResource implements WritableResource {

	/**
	 * Object key used only to build a bucket URL with {@link S3Client#utilities()}, which rejects blank keys. The
	 * object is never requested.
	 */
	private static final String BUCKET_URL_PLACEHOLDER_KEY = "placeholder";

	protected final Location location;

	protected final S3Client s3Client;

	protected final S3OutputStreamProvider s3OutputStreamProvider;

	@Nullable
	private HeadMetadata headMetadata;

	@Nullable
	private ObjectMetadata objectMetadata;

	@Nullable
	public static S3Resource create(String location, S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider) {
		if (Location.isSimpleStorageResource(location)) {
			return new S3Resource(location, s3Client, s3OutputStreamProvider);
		}
		return null;
	}

	public S3Resource(String location, S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider) {
		this(Location.of(location), s3Client, s3OutputStreamProvider);
	}

	public S3Resource(String bucket, String key, S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider) {
		this(Location.of(bucket, key), s3Client, s3OutputStreamProvider);
	}

	public S3Resource(Location location, S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider) {
		Assert.notNull(location, "location is required");
		Assert.notNull(s3Client, "s3Client is required");
		Assert.notNull(s3OutputStreamProvider, "s3OutputStreamProvider is required");

		this.location = location;
		this.s3Client = s3Client;
		this.s3OutputStreamProvider = s3OutputStreamProvider;
	}

	@Override
	public URL getURL() throws IOException {
		if (!StringUtils.hasText(this.location.getObject())) {
			return getBucketUrl();
		}
		GetUrlRequest getUrlRequest = GetUrlRequest.builder().bucket(this.getLocation().getBucket())
				.key(this.location.getObject()).versionId(this.location.getVersion()).build();
		return s3Client.utilities().getUrl(getUrlRequest);
	}

	/**
	 * Resolves the bucket URL against the URL of an object in the bucket, since
	 * {@link software.amazon.awssdk.services.s3.S3Utilities} only builds object URLs. This keeps the endpoint, region
	 * and addressing style of the configured {@link S3Client}.
	 */
	private URL getBucketUrl() throws IOException {
		URL objectUrl = s3Client.utilities()
				.getUrl(GetUrlRequest.builder().bucket(location.getBucket()).key(BUCKET_URL_PLACEHOLDER_KEY).build());
		try {
			return objectUrl.toURI().resolve(".").toURL();
		}
		catch (URISyntaxException ex) {
			throw new IOException("Could not resolve the URL of bucket " + location.getBucket() + " from " + objectUrl,
					ex);
		}
	}

	@Override
	public String getDescription() {
		return location.toString();
	}

	@Override
	public S3Resource createRelative(String relativePath) {
		return new S3Resource(location.relative(relativePath), this.s3Client, this.s3OutputStreamProvider);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return s3Client.getObject(request -> request.bucket(location.getBucket()).key(location.getObject())
				.versionId(location.getVersion()));
	}

	public String contentType() {
		if (headMetadata == null) {
			fetchMetadata();
		}
		return headMetadata.contentType;
	}

	public void setObjectMetadata(@Nullable ObjectMetadata objectMetadata) {
		this.objectMetadata = objectMetadata;
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
	public long contentLength() {
		if (headMetadata == null) {
			fetchMetadata();
		}
		return headMetadata.contentLength;
	}

	@Override
	public long lastModified() {
		if (headMetadata == null) {
			fetchMetadata();
		}
		return headMetadata.lastModified.toEpochMilli();
	}

	@Override
	public File getFile() {
		throw new UnsupportedOperationException("Amazon S3 resource can not be resolved to java.io.File objects.Use "
				+ "getInputStream() to retrieve the contents of the object!");
	}

	public Map<String, String> metadata() {
		if (headMetadata == null) {
			fetchMetadata();
		}
		return headMetadata.metadata;
	}

	private void fetchMetadata() {
		HeadObjectResponse response = s3Client.headObject(request -> request.bucket(location.getBucket())
				.key(location.getObject()).versionId(location.getVersion()));
		this.headMetadata = new HeadMetadata(response);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return s3OutputStreamProvider.create(location.getBucket(), location.getObject(), objectMetadata);
	}

	@Override
	public String getFilename() {
		return this.location.getObject();
	}

	public Location getLocation() {
		return location;
	}

	private static class HeadMetadata {

		private final Long contentLength;

		private final Instant lastModified;

		private final String contentType;

		private final Map<String, String> metadata;

		HeadMetadata(HeadObjectResponse headObjectResponse) {
			this.contentLength = headObjectResponse.contentLength();
			this.lastModified = headObjectResponse.lastModified();
			this.contentType = headObjectResponse.contentType();
			this.metadata = headObjectResponse.metadata();
		}

	}

}
