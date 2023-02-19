/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Unit tests for {@link S3Resource}.
 *
 * @author Maciej Walkowiak
 */
class S3ResourceTests {

	@Test
	void createsRelativeResourceWhenObjectIsSet() {
		S3Resource resource = new S3Resource("bucket", "object", mock(S3Client.class),
				mock(S3OutputStreamProvider.class));
		S3Resource result = resource.createRelative("foo");
		assertThat(result.getLocation()).isEqualTo(Location.of("bucket", "object/foo"));
	}

	@Test
	void createsRelativeResourceWhenObjectIsNotSet() {
		S3Resource resource = new S3Resource("bucket", "", mock(S3Client.class), mock(S3OutputStreamProvider.class));
		S3Resource result = resource.createRelative("foo");
		assertThat(result.getLocation()).isEqualTo(Location.of("bucket", "foo"));
	}

}
