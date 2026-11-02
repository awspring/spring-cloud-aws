/*
 * Copyright 2013-2026 the original author or authors.
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

import io.awspring.cloud.s3.LocalstackContainerTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * An Amazon S3 {@link Session} implementation tests.
 * @author Karol Kosiacki
 * 
 */
public class S3SessionTests implements LocalstackContainerTest {

	static S3Client S3;

	// define the bucket and file name used throughout the test
	static final String S3_BUCKET_NAME = "my-bucket";

	static final String S3_FILE_KEY_BAR = "subdir/bar";

	@BeforeAll
	static void setup() {
		S3 = LocalstackContainerTest.s3Client();
		S3.createBucket(request -> request.bucket(S3_BUCKET_NAME));
		S3.putObject(request -> request.bucket(S3_BUCKET_NAME).key(S3_FILE_KEY_BAR), RequestBody.fromString("Bar"));
	}

	@Test
	void shouldReturnTrueOnExistingObject() {
		try (S3Session s3Session = new S3Session(S3)) {
			Assertions.assertThat(s3Session.exists(S3_BUCKET_NAME + '/' + S3_FILE_KEY_BAR)).isTrue();
		}
		finally {
			S3.close();
		}
	}
}
