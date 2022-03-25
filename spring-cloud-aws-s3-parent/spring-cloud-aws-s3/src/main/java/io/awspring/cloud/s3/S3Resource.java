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

import java.io.IOException;
import java.io.InputStream;

import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.core.io.AbstractResource;

public class S3Resource extends AbstractResource {

	private final Location location;

	private final S3Client s3Client;

	public static S3Resource create(String location, S3Client s3Client) {
		if (Location.isSimpleStorageResource(location)) {
			return new S3Resource(location, s3Client);
		}
		return null;
	}

	S3Resource(String location, S3Client s3Client) {
		this.location = Location.of(location);
		this.s3Client = s3Client;
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

}
