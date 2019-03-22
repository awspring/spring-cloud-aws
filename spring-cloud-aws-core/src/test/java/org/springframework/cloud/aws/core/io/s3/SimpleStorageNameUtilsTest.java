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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getBucketNameFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getLocationForBucketAndObject;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getLocationForBucketAndObjectAndVersionId;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getObjectNameFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getVersionIdFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.isSimpleStorageResource;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.stripProtocol;

/**
 * @author Agim Emruli
 */
public class SimpleStorageNameUtilsTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testIsSimpleStorageResource() throws Exception {
		assertThat(isSimpleStorageResource("s3://foo/bar")).isTrue();
		assertThat(isSimpleStorageResource("S3://foo/bar")).isTrue();
		assertThat(isSimpleStorageResource("f3://foo/bar")).isFalse();
		assertThat(isSimpleStorageResource("s4://foo/bar")).isFalse();
	}

	@Test
	public void testIsSimpleStorageResourceNUll() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		isSimpleStorageResource(null);
	}

	@Test
	public void testGetBucketNameFromLocation() throws Exception {
		assertThat(getBucketNameFromLocation("s3://foo/bar")).isEqualTo("foo");
		assertThat(getBucketNameFromLocation("s3://fo*/bar")).isEqualTo("fo*");

		assertThat(getBucketNameFromLocation("s3://foo/bar/baz/boo/")).isEqualTo("foo");
		assertThat(getBucketNameFromLocation("s3://fo*/bar/baz/boo/")).isEqualTo("fo*");

		assertThat(getBucketNameFromLocation("s3://foo/bar/baz/boo/^versionIdValue"))
				.isEqualTo("foo");
	}

	@Test
	public void testGetBucketNameNotNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		getBucketNameFromLocation(null);
	}

	@Test
	public void testNonLocationForBucket() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("not a valid S3 location");
		getBucketNameFromLocation("foo://fo*");
	}

	@Test
	public void testNonValidBucket() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		getBucketNameFromLocation("s3://fo*");
	}

	@Test
	public void testEmptyValidBucket() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		getBucketNameFromLocation("s3:///");
	}

	@Test
	public void testGetObjectNameNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		getObjectNameFromLocation(null);
	}

	@Test
	public void testGetObjectNameFromLocation() throws Exception {
		assertThat(getObjectNameFromLocation("s3://foo/bar")).isEqualTo("bar");
		assertThat(getObjectNameFromLocation("s3://foo/ba*")).isEqualTo("ba*");
		assertThat(getObjectNameFromLocation("s3://foo/")).isEqualTo("");
		assertThat(getObjectNameFromLocation("s3://foo/bar^versionIdValue"))
				.isEqualTo("bar");

		assertThat(getObjectNameFromLocation("s3://foo/bar/baz/boo/"))
				.isEqualTo("bar/baz/boo");
		assertThat(getObjectNameFromLocation("s3://foo/bar/ba*/boo/"))
				.isEqualTo("bar/ba*/boo");
		assertThat(getObjectNameFromLocation("s3://foo/bar/baz/boo.txt/"))
				.isEqualTo("bar/baz/boo.txt");
		assertThat(getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/"))
				.isEqualTo("bar/ba*/boo.txt");
		assertThat(getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue"))
				.isEqualTo("bar/ba*/boo.txt");
	}

	@Test
	public void testGetVersionIdFromLocation() throws Exception {
		assertThat(getVersionIdFromLocation("s3://foo/bar^versionIdValue"))
				.isEqualTo("versionIdValue");
		assertThat(getVersionIdFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue"))
				.isEqualTo("versionIdValue");
	}

	@Test
	public void testEmptyValidBucketForLocation() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		getObjectNameFromLocation("s3:///");
	}

	@Test
	public void testEmptyValidBucketForLocationWithKey() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		getObjectNameFromLocation("s3:///foo");
	}

	@Test
	public void testGetLocationForObjectNameAndBucket() throws Exception {
		assertThat(getLocationForBucketAndObject("foo", "bar")).isEqualTo("s3://foo/bar");
		assertThat(getLocationForBucketAndObject("foo", "bar/baz"))
				.isEqualTo("s3://foo/bar/baz");
		assertThat(getLocationForBucketAndObject("foo", "bar/baz.txt"))
				.isEqualTo("s3://foo/bar/baz.txt");
	}

	@Test
	public void testGetLocationForObjectNameAndBucketAndVersionId() throws Exception {
		assertThat(
				getLocationForBucketAndObjectAndVersionId("foo", "bar", "versionIdValue"))
						.isEqualTo("s3://foo/bar^versionIdValue");
		assertThat(getLocationForBucketAndObjectAndVersionId("foo", "bar/baz",
				"versionIdValue")).isEqualTo("s3://foo/bar/baz^versionIdValue");
		assertThat(getLocationForBucketAndObjectAndVersionId("foo", "bar/baz.txt",
				"versionIdValue")).isEqualTo("s3://foo/bar/baz.txt^versionIdValue");
	}

	@Test
	public void testStripProtocol() throws Exception {
		assertThat(stripProtocol("s3://foo/bar")).isEqualTo("foo/bar");
		assertThat(stripProtocol("s3://foo/bar/baz")).isEqualTo("foo/bar/baz");
		assertThat(stripProtocol("s3://foo/bar.txt")).isEqualTo("foo/bar.txt");
		assertThat(stripProtocol("s3://foo/bar.txt^versionIdValue"))
				.isEqualTo("foo/bar.txt^versionIdValue");
		assertThat(stripProtocol("s3://")).isEqualTo("");
	}

	@Test
	public void testStripProtocolNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		stripProtocol(null);
	}

}
