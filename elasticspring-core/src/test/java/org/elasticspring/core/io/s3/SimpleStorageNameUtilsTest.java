/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.core.io.s3;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Agim Emruli
 */
public class SimpleStorageNameUtilsTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testIsSimpleStorageResource() throws Exception {
		Assert.assertTrue(SimpleStorageNameUtils.isSimpleStorageResource("s3://foo/bar"));
		Assert.assertTrue(SimpleStorageNameUtils.isSimpleStorageResource("S3://foo/bar"));
		Assert.assertFalse(SimpleStorageNameUtils.isSimpleStorageResource("f3://foo/bar"));
		Assert.assertFalse(SimpleStorageNameUtils.isSimpleStorageResource("s4://foo/bar"));
	}

	@Test
	public void testIsSimpleStorageResourceNUll() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		SimpleStorageNameUtils.isSimpleStorageResource(null);
	}

	@Test
	public void testGetBucketNameFromLocation() throws Exception {
		Assert.assertEquals("foo", SimpleStorageNameUtils.getBucketNameFromLocation("s3://foo/bar"));
		Assert.assertEquals("fo*", SimpleStorageNameUtils.getBucketNameFromLocation("s3://fo*/bar"));

		Assert.assertEquals("foo", SimpleStorageNameUtils.getBucketNameFromLocation("s3://foo/bar/baz/boo/"));
		Assert.assertEquals("fo*", SimpleStorageNameUtils.getBucketNameFromLocation("s3://fo*/bar/baz/boo/"));
	}

	@Test
	public void testGetBucketNameNotNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		SimpleStorageNameUtils.getBucketNameFromLocation(null);
	}

	@Test
	public void testNonLocationForBucket() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("not a valid S3 location");
		SimpleStorageNameUtils.getBucketNameFromLocation("foo://fo*");
	}

	@Test
	public void testNonValidBucket() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		SimpleStorageNameUtils.getBucketNameFromLocation("s3://fo*");
	}

	@Test
	public void testEmptyValidBucket() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		SimpleStorageNameUtils.getBucketNameFromLocation("s3:///");
	}

	@Test
	public void testGetObjectNameNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		SimpleStorageNameUtils.getObjectNameFromLocation(null);
	}

	@Test
	public void testGetObjectNameFromLocation() throws Exception {
		Assert.assertEquals("bar", SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar"));
		Assert.assertEquals("ba*", SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/ba*"));

		Assert.assertEquals("bar/baz/boo", SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/baz/boo/"));
		Assert.assertEquals("bar/ba*/boo", SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/ba*/boo/"));
		Assert.assertEquals("bar/baz/boo.txt", SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/baz/boo.txt/"));
		Assert.assertEquals("bar/ba*/boo.txt", SimpleStorageNameUtils.getObjectNameFromLocation("s3://foo/bar/ba*/boo.txt/"));
	}

	@Test
	public void testEmptyValidBucketForLocation() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		SimpleStorageNameUtils.getObjectNameFromLocation("s3:///");
	}

	@Test
	public void testEmptyValidBucketForLocationWithKey() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("valid bucket name");
		SimpleStorageNameUtils.getObjectNameFromLocation("s3:///foo");
	}

	@Test
	public void testGetLocationForObjectNameAndBucket() throws Exception {
		Assert.assertEquals("s3://foo/bar", SimpleStorageNameUtils.getLocationForBucketAndObject("foo", "bar"));
		Assert.assertEquals("s3://foo/bar/baz", SimpleStorageNameUtils.getLocationForBucketAndObject("foo", "bar/baz"));
		Assert.assertEquals("s3://foo/bar/baz.txt", SimpleStorageNameUtils.getLocationForBucketAndObject("foo", "bar/baz.txt"));
	}

	@Test
	public void testStripProtocol() throws Exception {
		Assert.assertEquals("foo/bar", SimpleStorageNameUtils.stripProtocol("s3://foo/bar"));
		Assert.assertEquals("foo/bar/baz", SimpleStorageNameUtils.stripProtocol("s3://foo/bar/baz"));
		Assert.assertEquals("foo/bar.txt", SimpleStorageNameUtils.stripProtocol("s3://foo/bar.txt"));
		Assert.assertEquals("", SimpleStorageNameUtils.stripProtocol("s3://"));
	}

	@Test
	public void testStripProtocolNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(" must not be null");
		SimpleStorageNameUtils.stripProtocol(null);
	}
}
