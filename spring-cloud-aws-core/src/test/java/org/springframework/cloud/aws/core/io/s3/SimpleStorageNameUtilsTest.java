/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getBucketNameFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getLocationForBucketAndObject;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getLocationForBucketAndObjectAndVersionId;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getObjectNameFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.getVersionIdFromLocation;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.isSimpleStorageResource;
import static org.springframework.cloud.aws.core.io.s3.SimpleStorageNameUtils.stripProtocol;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Agim Emruli
 */
public class SimpleStorageNameUtilsTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testIsSimpleStorageResource() throws Exception {
		assertTrue(isSimpleStorageResource("s3://foo/bar"));
		assertTrue(isSimpleStorageResource("S3://foo/bar"));
		assertFalse(isSimpleStorageResource("f3://foo/bar"));
		assertFalse(isSimpleStorageResource("s4://foo/bar"));
	}

	@Test
	public void testIsSimpleStorageResourceNUll() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		isSimpleStorageResource(null);
	}

	@Test
	public void testGetBucketNameFromLocation() throws Exception {
		assertEquals("foo", getBucketNameFromLocation("s3://foo/bar"));
		assertEquals("fo*", getBucketNameFromLocation("s3://fo*/bar"));

		assertEquals("foo", getBucketNameFromLocation("s3://foo/bar/baz/boo/"));
		assertEquals("fo*", getBucketNameFromLocation("s3://fo*/bar/baz/boo/"));
		
		assertEquals("foo", getBucketNameFromLocation("s3://foo/bar/baz/boo/^versionIdValue"));
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
		assertEquals("bar", getObjectNameFromLocation("s3://foo/bar"));
		assertEquals("ba*", getObjectNameFromLocation("s3://foo/ba*"));
		assertEquals("bar", getObjectNameFromLocation("s3://foo/bar^versionIdValue"));

		assertEquals("bar/baz/boo", getObjectNameFromLocation("s3://foo/bar/baz/boo/"));
		assertEquals("bar/ba*/boo", getObjectNameFromLocation("s3://foo/bar/ba*/boo/"));
		assertEquals("bar/baz/boo.txt", getObjectNameFromLocation("s3://foo/bar/baz/boo.txt/"));
		assertEquals("bar/ba*/boo.txt", getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/"));
		assertEquals("bar/ba*/boo.txt", getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue"));
	}
	
	@Test
	public void testGetVersionIdFromLocation() throws Exception {
		assertEquals("versionIdValue", getVersionIdFromLocation("s3://foo/bar^versionIdValue"));
		assertEquals("versionIdValue", getVersionIdFromLocation("s3://foo/bar/ba*/boo.txt/^versionIdValue"));
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
		assertEquals("s3://foo/bar", getLocationForBucketAndObject("foo", "bar"));
		assertEquals("s3://foo/bar/baz", getLocationForBucketAndObject("foo", "bar/baz"));
		assertEquals("s3://foo/bar/baz.txt", getLocationForBucketAndObject("foo", "bar/baz.txt"));
	}
	
	@Test
	public void testGetLocationForObjectNameAndBucketAndVersionId() throws Exception {
		assertEquals("s3://foo/bar^versionIdValue", getLocationForBucketAndObjectAndVersionId("foo", "bar", "versionIdValue"));
		assertEquals("s3://foo/bar/baz^versionIdValue", getLocationForBucketAndObjectAndVersionId("foo", "bar/baz", "versionIdValue"));
		assertEquals("s3://foo/bar/baz.txt^versionIdValue", getLocationForBucketAndObjectAndVersionId("foo", "bar/baz.txt", "versionIdValue"));
	}

	@Test
	public void testStripProtocol() throws Exception {
		assertEquals("foo/bar", stripProtocol("s3://foo/bar"));
		assertEquals("foo/bar/baz", stripProtocol("s3://foo/bar/baz"));
		assertEquals("foo/bar.txt", stripProtocol("s3://foo/bar.txt"));
		assertEquals("foo/bar.txt^versionIdValue", stripProtocol("s3://foo/bar.txt^versionIdValue"));
		assertEquals("", stripProtocol("s3://"));
	}

	@Test
	public void testStripProtocolNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		stripProtocol(null);
	}
}
