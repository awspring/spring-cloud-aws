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

import org.jspecify.annotations.Nullable;

/**
 * Thrown when uploading to S3 fails.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class UploadFailedException extends S3Exception {

	/**
	 * A path to temporary location containing a file that has not been uploaded to S3.
	 */
	@Nullable
	private final String path;

	public UploadFailedException(@Nullable String path, @Nullable Exception se) {
		super("Upload failed. File is stored in a temporary folder in the filesystem " + path, se);
		this.path = path;
	}

	@Nullable
	public String getPath() {
		return path;
	}

}
