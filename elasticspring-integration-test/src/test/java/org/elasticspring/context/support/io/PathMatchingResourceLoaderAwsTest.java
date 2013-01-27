/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.context.support.io;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("PathMatchingResourceLoaderAwsTest-context.xml")
public class PathMatchingResourceLoaderAwsTest {

	@SuppressWarnings("UnusedDeclaration")
	@Autowired
	private TestBucketsInitialization testBucketsInitialization;

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	ResourcePatternResolver resourceLoader;

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWildcardsInKey() throws IOException, InterruptedException {
		final String protocolAndBucket = "s3://my-bucket-one.elasticspring.org";

		{
			Resource[] resources = resourceLoader.getResources(protocolAndBucket + "/foo1/bar?/test1.txt");
			Assert.assertEquals("test the '?' wildcard", 1, resources.length);
		}

		{
			Resource[] resources = resourceLoader.getResources(protocolAndBucket + "/foo*/bar2/test2.txt");
			Assert.assertEquals("test the '*' wildcard", 1, resources.length);
		}

		{
			Resource[] resources = resourceLoader.getResources(protocolAndBucket + "/**/test1.txt");
			Assert.assertEquals("test the '**' wildcard", 4, resources.length);
		}

		{
			Resource[] resources = resourceLoader.getResources(protocolAndBucket + "/**/test?.txt");
			Assert.assertEquals("test a mix of '**' and '?'", 6, resources.length);
		}

		{
			Resource[] resources = resourceLoader.getResources(protocolAndBucket + "/**/baz*/test?.txt");
			Assert.assertEquals("test all together", 2, resources.length);
		}
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testWildcardsInBucket() throws IOException, InterruptedException {
		{
			Resource[] resources = resourceLoader.getResources("s3://my-bucket-?ne.elasticspring.org/test1.txt");
			Assert.assertEquals("test the '?' wildcard", 1, resources.length);
		}

		{
			Resource[] resources = resourceLoader.getResources("s3://my-bucket*/test1.txt");
			Assert.assertEquals("test the '*' wildcard", 2, resources.length);
		}

		{
			Resource[] resources = resourceLoader.getResources("s3://**/test1.txt");
			Assert.assertEquals("test the '**' wildcard", 16, resources.length);
		}
	}
}
