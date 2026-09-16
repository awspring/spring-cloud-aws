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
package io.awspring.cloud.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Matej Nedic
 */
class LocationTests {

	@Test
	void resolvesBucketAndObject() {
		Location location = Location.of("s3://my-bucket/file.txt");

		assertThat(location.getBucket()).isEqualTo("my-bucket");
		assertThat(location.getObject()).isEqualTo("file.txt");
		assertThat(location.getVersion()).isNull();
	}

	@Test
	void resolvesNestedObject() {
		Location location = Location.of("s3://my-bucket/path/file.txt");

		assertThat(location.getBucket()).isEqualTo("my-bucket");
		assertThat(location.getObject()).isEqualTo("path/file.txt");
	}

	@Test
	void bucketOnlyLocationHasEmptyObject() {
		Location location = Location.of("s3://my-bucket/");

		assertThat(location.getBucket()).isEqualTo("my-bucket");
		assertThat(location.getObject()).isEmpty();
	}

	@Test
	void retainsTrailingSlashForPrefixLocation() {
		Location location = Location.of("s3://my-bucket/path/");

		assertThat(location.getBucket()).isEqualTo("my-bucket");
		assertThat(location.getObject()).isEqualTo("path/");
	}

	@Test
	void retainsTrailingSlashForNestedPrefixLocation() {
		Location location = Location.of("s3://my-bucket/path/sub/");

		assertThat(location.getObject()).isEqualTo("path/sub/");
	}

	@Test
	void resolvesVersionId() {
		Location location = Location.of("s3://my-bucket/file.txt^version-id");

		assertThat(location.getBucket()).isEqualTo("my-bucket");
		assertThat(location.getObject()).isEqualTo("file.txt");
		assertThat(location.getVersion()).isEqualTo("version-id");
	}

	@Test
	void throwsWhenLocationHasNoObjectDelimiter() {
		assertThatThrownBy(() -> Location.of("s3://my-bucket")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not contain a valid bucket name");
	}

	@Test
	void throwsWhenBucketNameIsEmpty() {
		assertThatThrownBy(() -> Location.of("s3:///file.txt")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not contain a valid bucket name");
	}

	@Test
	void prefixAndObjectLocationsAreNotEqual() {
		Location prefix = Location.of("s3://my-bucket/path/");
		Location object = Location.of("s3://my-bucket/path");

		assertThat(prefix).isNotEqualTo(object);
		assertThat(prefix.hashCode()).isNotEqualTo(object.hashCode());
	}

	@Test
	void relativeAppendsSlashWhenObjectHasNoTrailingSlash() {
		Location location = Location.of("my-bucket", "path");

		assertThat(location.relative("file.txt").getObject()).isEqualTo("path/file.txt");
	}

	@Test
	void relativeDoesNotDoubleSlashWhenObjectEndsWithSlash() {
		Location location = Location.of("my-bucket", "path/");

		assertThat(location.relative("file.txt").getObject()).isEqualTo("path/file.txt");
	}

	@Test
	void relativeUsesRelativePathWhenObjectIsEmpty() {
		Location location = Location.of("my-bucket", "");

		assertThat(location.relative("file.txt").getObject()).isEqualTo("file.txt");
	}

	@Test
	void relativeFromParsedPrefixLocationDoesNotDoubleSlash() {
		Location location = Location.of("s3://my-bucket/path/");

		Location relative = location.relative("file.txt");

		assertThat(relative.getBucket()).isEqualTo("my-bucket");
		assertThat(relative.getObject()).isEqualTo("path/file.txt");
	}

}
