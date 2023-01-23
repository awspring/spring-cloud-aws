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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Represents S3 bucket or object location.
 *
 * @author Maciej Walkowiak
 * @author Tobias Soloschenko
 * @since 3.0
 */
public class Location {

	public static final String S3_PROTOCOL_PREFIX = "s3://";

	public static final String PATH_DELIMITER = "/";

	public static final String VERSION_DELIMITER = "^";

	private final String bucket;

	private final String object;

	@Nullable
	private final String version;

	/**
	 * Creates {@link Location} from full S3 path s3://bucket-name/object-key.
	 * @param location - the location
	 * @return {@link Location}
	 */
	public static Location of(String location) {
		return new Location(location);
	}

	/**
	 * Creates {@link Location} from bucket (bucket-name)/ object (object-key)
	 * @param bucket - the bucket
	 * @param object - the object key
	 * @return {@link Location}
	 */
	public static Location of(String bucket, String object) {
		return new Location(bucket, object);
	}

	/**
	 * Creates location.
	 * @param bucket - the bucket name
	 * @param object - the object key
	 */
	Location(String bucket, String object) {
		this(bucket, object, null);
	}

	/**
	 * Creates location.
	 * @param bucket - the bucket name
	 * @param object - the object key
	 * @param version - the object version
	 */
	Location(String bucket, String object, @Nullable String version) {
		Assert.notNull(bucket, "bucket is required");
		Assert.notNull(object, "object is required");

		this.bucket = bucket;
		this.object = object;
		this.version = version;
	}

	/**
	 * Creates {@link Location} from full S3 path s3://bucket-name/object-key.
	 * @param location - the location
	 */
	private Location(String location) {
		Assert.notNull(location, "location is required");

		this.bucket = resolveBucketName(location);
		this.object = resolveObjectName(location);
		this.version = resolveVersionId(location);
	}

	public String getBucket() {
		return bucket;
	}

	public String getObject() {
		return object;
	}

	@Nullable
	public String getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "Location{" + "bucket='" + bucket + '\'' + ", object='" + object + '\'' + ", version='" + version + '\''
				+ '}';
	}

	static boolean isSimpleStorageResource(String location) {
		Assert.notNull(location, "Location must not be null");
		return location.toLowerCase().startsWith(S3_PROTOCOL_PREFIX);
	}

	private static String resolveBucketName(String location) {
		int bucketEndIndex = location.indexOf(PATH_DELIMITER, S3_PROTOCOL_PREFIX.length());
		if (bucketEndIndex == -1 || bucketEndIndex == S3_PROTOCOL_PREFIX.length()) {
			throw new IllegalArgumentException("The location :'" + location + "' does not contain a valid bucket name");
		}
		return location.substring(S3_PROTOCOL_PREFIX.length(), bucketEndIndex);
	}

	private static String resolveObjectName(String location) {
		int bucketEndIndex = location.indexOf(PATH_DELIMITER, S3_PROTOCOL_PREFIX.length());
		if (bucketEndIndex == -1 || bucketEndIndex == S3_PROTOCOL_PREFIX.length()) {
			throw new IllegalArgumentException("The location :'" + location + "' does not contain a valid bucket name");
		}

		if (location.contains(VERSION_DELIMITER)) {
			return resolveObjectName(location.substring(0, location.indexOf(VERSION_DELIMITER)));
		}

		int endIndex = location.length();
		if (location.endsWith(PATH_DELIMITER)) {
			endIndex--;
		}

		if (bucketEndIndex >= endIndex) {
			return "";
		}

		return location.substring(++bucketEndIndex, endIndex);
	}

	@Nullable
	private static String resolveVersionId(String location) {
		int objectNameEndIndex = location.indexOf(VERSION_DELIMITER, S3_PROTOCOL_PREFIX.length());
		if (objectNameEndIndex == -1 || location.endsWith(VERSION_DELIMITER)) {
			return null;
		}

		if (objectNameEndIndex == S3_PROTOCOL_PREFIX.length()) {
			throw new IllegalArgumentException("The location :'" + location + "' does not contain a valid bucket name");
		}

		return location.substring(++objectNameEndIndex);
	}

}
