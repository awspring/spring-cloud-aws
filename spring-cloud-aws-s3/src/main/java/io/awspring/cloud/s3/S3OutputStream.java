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
import java.io.OutputStream;

/**
 * Represents {@link OutputStream} that writes data to S3.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public abstract class S3OutputStream extends OutputStream {

	/**
	 * Cancels the upload and cleans up temporal resources (temp files, partial multipart upload).
	 */
	public void abort() throws IOException {
	}
}
