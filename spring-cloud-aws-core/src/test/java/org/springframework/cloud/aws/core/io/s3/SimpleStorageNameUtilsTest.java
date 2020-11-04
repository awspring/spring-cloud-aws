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

package org.springframework.cloud.aws.core.io.s3;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getBucketNameFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getContentTypeFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getLocationForBucketAndObject;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getLocationForBucketAndObjectAndVersionId;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getObjectNameFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getVersionIdFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.isSimpleStorageResource;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.stripProtocol;

/**
 * @author Agim Emruli
 */
class SimpleStorageNameUtilsTest {

	@Test
	void testIsSimpleStorageResource() throws Exception {
		assertThat(isSimpleStorageResource("s3://foo/bar")).isTrue();
		assertThat(isSimpleStorageResource("S3://foo/bar")).isTrue();
		assertThat(isSimpleStorageResource("f3://foo/bar")).isFalse();
		assertThat(isSimpleStorageResource("s4://foo/bar")).isFalse();
	}

	@Test
	void testIsSimpleStorageResourceNUll() throws Exception {
		assertThatThrownBy(() -> isSimpleStorageResource(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(" must not be null");
	}

	@Test
	void testGetBucketNameFromLocation() throws Exception {
		assertThat(getBucketNameFromLocation("s3://foo/bar")).isEqualTo("foo");
		assertThat(getBucketNameFromLocation("s3://fo*/bar")).isEqualTo("fo*");

		assertThat(getBucketNameFromLocation("s3://foo/bar/baz/boo/")).isEqualTo("foo");
		assertThat(getBucketNameFromLocation("s3://fo*/bar/baz/boo/")).isEqualTo("fo*");

		assertThat(getBucketNameFromLocation("s3://foo/bar/baz/boo/^versionIdValue")).isEqualTo("foo");
	}

	@Test
	void testGetBucketNameNotNull() throws Exception {
		assertThatThrownBy(() -> getBucketNameFromLocation(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(" must not be null");
	}

	@Test
	void testNonLocationForBucket() throws Exception {
		assertThatThrownBy(() -> getBucketNameFromLocation("foo://fo*")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not a valid S3 location");
	}

	@Test
	void testNonValidBucket() throws Exception {
		assertThatThrownBy(() -> getBucketNameFromLocation("s3://fo*")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("valid bucket name");
	}

	@Test
	void testEmptyValidBucket() throws Exception {
		assertThatThrownBy(() -> getBucketNameFromLocation("s3:///")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("valid bucket name");
	}

	@Test
	void testGetObjectNameNull() throws Exception {
		assertThatThrownBy(() -> getObjectNameFromLocation(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(" must not be null");
	}

	@Test
	void testGetObjectNameFromLocation() throws Exception {
		assertThat(getObjectNameFromLocation("s3://foo/bar")).isEqualTo("bar");
		assertThat(getObjectNameFromLocation("s3://foo/ba*")).isEqualTo("ba*");
		assertThat(getObjectNameFromLocation("s3://foo/")).isEqualTo("");
		assertThat(getObjectNameFromLocation("s3://foo/bar^versionIdValue")).isEqualTo("bar");

		assertThat(getObjectNameFromLocation("s3://foo/bar/baz/boo/")).isEqualTo("bar/baz/boo");
		assertThat(getObjectNameFromLocation("s3://foo/bar/ba*/boo/")).isEqualTo("bar/ba*/boo");
		assertThat(getObjectNameFromLocation("s3://foo/bar/baz/boo.txt/")).isEqualTo("bar/baz/boo.txt");
		assertThat(getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/")).isEqualTo("bar/ba*/boo.txt");
		assertThat(getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue")).isEqualTo("bar/ba*/boo.txt");
	}

	@Test
	void testGetVersionIdFromLocation() throws Exception {
		assertThat(getVersionIdFromLocation("s3://foo/bar^versionIdValue")).isEqualTo("versionIdValue");
		assertThat(getVersionIdFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue")).isEqualTo("versionIdValue");
	}

	@Test
	public void testGetContentTypeFromLocation() {
		assertThat(getContentTypeFromLocation("s3://foo/bar")).isEqualTo(null);
		assertThat(getContentTypeFromLocation("s3://foo/bar^versionIdValue")).isEqualTo(null);
		assertThat(getContentTypeFromLocation("s3://foo/bar/baz/boo.txt")).isEqualTo("text/plain");
		assertThat(getContentTypeFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue")).isEqualTo("text/plain");
	}

	@Test
	void testEmptyValidBucketForLocation() throws Exception {
		assertThatThrownBy(() -> getObjectNameFromLocation("s3:///")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("valid bucket name");
	}

	@Test
	void testEmptyValidBucketForLocationWithKey() throws Exception {
		assertThatThrownBy(() -> getObjectNameFromLocation("s3:///foo")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("valid bucket name");
	}

	@Test
	void testGetLocationForObjectNameAndBucket() throws Exception {
		assertThat(getLocationForBucketAndObject("foo", "bar")).isEqualTo("s3://foo/bar");
		assertThat(getLocationForBucketAndObject("foo", "bar/baz")).isEqualTo("s3://foo/bar/baz");
		assertThat(getLocationForBucketAndObject("foo", "bar/baz.txt")).isEqualTo("s3://foo/bar/baz.txt");
	}

	@Test
	void testGetLocationForObjectNameAndBucketAndVersionId() throws Exception {
		assertThat(getLocationForBucketAndObjectAndVersionId("foo", "bar", "versionIdValue"))
				.isEqualTo("s3://foo/bar^versionIdValue");
		assertThat(getLocationForBucketAndObjectAndVersionId("foo", "bar/baz", "versionIdValue"))
				.isEqualTo("s3://foo/bar/baz^versionIdValue");
		assertThat(getLocationForBucketAndObjectAndVersionId("foo", "bar/baz.txt", "versionIdValue"))
				.isEqualTo("s3://foo/bar/baz.txt^versionIdValue");
	}

	@Test
	void testStripProtocol() throws Exception {
		assertThat(stripProtocol("s3://foo/bar")).isEqualTo("foo/bar");
		assertThat(stripProtocol("s3://foo/bar/baz")).isEqualTo("foo/bar/baz");
		assertThat(stripProtocol("s3://foo/bar.txt")).isEqualTo("foo/bar.txt");
		assertThat(stripProtocol("s3://foo/bar.txt^versionIdValue")).isEqualTo("foo/bar.txt^versionIdValue");
		assertThat(stripProtocol("s3://")).isEqualTo("");
	}

	@Test
	void testStripProtocolNull() throws Exception {
		assertThatThrownBy(() -> stripProtocol(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(" must not be null");
	}

}
