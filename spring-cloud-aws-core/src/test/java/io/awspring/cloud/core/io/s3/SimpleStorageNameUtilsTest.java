/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.cloud.core.io.s3;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Agim Emruli
 */
class SimpleStorageNameUtilsTest {

	@Test
	void testIsSimpleStorageResource() throws Exception {
		Assertions.assertThat(SimpleStorageNameUtils.isSimpleStorageResource("s3://foo/bar")).isTrue();
		Assertions.assertThat(SimpleStorageNameUtils.isSimpleStorageResource("S3://foo/bar")).isTrue();
		Assertions.assertThat(SimpleStorageNameUtils.isSimpleStorageResource("f3://foo/bar")).isFalse();
		Assertions.assertThat(SimpleStorageNameUtils.isSimpleStorageResource("s4://foo/bar")).isFalse();
	}

	@Test
	void testIsSimpleStorageResourceNUll() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.isSimpleStorageResource(null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(" must not be null");
	}

	@Test
	void testGetBucketNameFromLocation() throws Exception {
		Assertions.assertThat(SimpleStorageNameUtils.getBucketNameFromLocation("s3://foo/bar")).isEqualTo("foo");
		Assertions.assertThat(SimpleStorageNameUtils.getBucketNameFromLocation("s3://fo*/bar")).isEqualTo("fo*");

		Assertions.assertThat(SimpleStorageNameUtils.getBucketNameFromLocation("s3://foo/bar/baz/boo/"))
				.isEqualTo("foo");
		Assertions.assertThat(SimpleStorageNameUtils.getBucketNameFromLocation("s3://fo*/bar/baz/boo/"))
				.isEqualTo("fo*");

		Assertions.assertThat(SimpleStorageNameUtils.getBucketNameFromLocation("s3://foo/bar/baz/boo/^versionIdValue"))
				.isEqualTo("foo");
	}

	@Test
	void testGetBucketNameNotNull() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.getBucketNameFromLocation(null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(" must not be null");
	}

	@Test
	void testNonLocationForBucket() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.getBucketNameFromLocation("foo://fo*"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not a valid S3 location");
	}

	@Test
	void testNonValidBucket() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.getBucketNameFromLocation("s3://fo*"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("valid bucket name");
	}

	@Test
	void testEmptyValidBucket() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.getBucketNameFromLocation("s3:///"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("valid bucket name");
	}

	@Test
	void testGetObjectNameNull() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.getObjectNameFromLocation(null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(" must not be null");
	}

	@Test
	void testGetObjectNameFromLocation() throws Exception {
		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar")).isEqualTo("bar");
		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/ba*")).isEqualTo("ba*");
		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/")).isEqualTo("");
		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar^versionIdValue"))
				.isEqualTo("bar");

		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/baz/boo/"))
				.isEqualTo("bar/baz/boo");
		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/ba*/boo/"))
				.isEqualTo("bar/ba*/boo");
		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/baz/boo.txt/"))
				.isEqualTo("bar/baz/boo.txt");
		Assertions.assertThat(SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/"))
				.isEqualTo("bar/ba*/boo.txt");
		Assertions
				.assertThat(
						SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue"))
				.isEqualTo("bar/ba*/boo.txt");
	}

	@Test
	void testGetVersionIdFromLocation() throws Exception {
		Assertions.assertThat(SimpleStorageNameUtils.getVersionIdFromLocation("s3://foo/bar^versionIdValue"))
				.isEqualTo("versionIdValue");
		Assertions
				.assertThat(SimpleStorageNameUtils.getVersionIdFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue"))
				.isEqualTo("versionIdValue");
	}

	@Test
	public void testGetContentTypeFromLocation() {
		Assertions.assertThat(SimpleStorageNameUtils.getContentTypeFromLocation("s3://foo/bar")).isEqualTo(null);
		Assertions.assertThat(SimpleStorageNameUtils.getContentTypeFromLocation("s3://foo/bar^versionIdValue"))
				.isEqualTo(null);
		Assertions.assertThat(SimpleStorageNameUtils.getContentTypeFromLocation("s3://foo/bar/baz/boo.txt"))
				.isEqualTo("text/plain");
		Assertions
				.assertThat(
						SimpleStorageNameUtils.getContentTypeFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue"))
				.isEqualTo("text/plain");
	}

	@Test
	void testEmptyValidBucketForLocation() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.getObjectNameFromLocation("s3:///"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("valid bucket name");
	}

	@Test
	void testEmptyValidBucketForLocationWithKey() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.getObjectNameFromLocation("s3:///foo"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("valid bucket name");
	}

	@Test
	void testGetLocationForObjectNameAndBucket() throws Exception {
		Assertions.assertThat(SimpleStorageNameUtils.getLocationForBucketAndObject("foo", "bar"))
				.isEqualTo("s3://foo/bar");
		Assertions.assertThat(SimpleStorageNameUtils.getLocationForBucketAndObject("foo", "bar/baz"))
				.isEqualTo("s3://foo/bar/baz");
		Assertions.assertThat(SimpleStorageNameUtils.getLocationForBucketAndObject("foo", "bar/baz.txt"))
				.isEqualTo("s3://foo/bar/baz.txt");
	}

	@Test
	void testGetLocationForObjectNameAndBucketAndVersionId() throws Exception {
		Assertions.assertThat(
				SimpleStorageNameUtils.getLocationForBucketAndObjectAndVersionId("foo", "bar", "versionIdValue"))
				.isEqualTo("s3://foo/bar^versionIdValue");
		Assertions.assertThat(
				SimpleStorageNameUtils.getLocationForBucketAndObjectAndVersionId("foo", "bar/baz", "versionIdValue"))
				.isEqualTo("s3://foo/bar/baz^versionIdValue");
		Assertions.assertThat(SimpleStorageNameUtils.getLocationForBucketAndObjectAndVersionId("foo", "bar/baz.txt",
				"versionIdValue")).isEqualTo("s3://foo/bar/baz.txt^versionIdValue");
	}

	@Test
	void testStripProtocol() throws Exception {
		Assertions.assertThat(SimpleStorageNameUtils.stripProtocol("s3://foo/bar")).isEqualTo("foo/bar");
		Assertions.assertThat(SimpleStorageNameUtils.stripProtocol("s3://foo/bar/baz")).isEqualTo("foo/bar/baz");
		Assertions.assertThat(SimpleStorageNameUtils.stripProtocol("s3://foo/bar.txt")).isEqualTo("foo/bar.txt");
		Assertions.assertThat(SimpleStorageNameUtils.stripProtocol("s3://foo/bar.txt^versionIdValue"))
				.isEqualTo("foo/bar.txt^versionIdValue");
		Assertions.assertThat(SimpleStorageNameUtils.stripProtocol("s3://")).isEqualTo("");
	}

	@Test
	void testStripProtocolNull() throws Exception {
		assertThatThrownBy(() -> SimpleStorageNameUtils.stripProtocol(null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(" must not be null");
	}

}
