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

package org.elasticspring.core.naming;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test for {@link AmazonResourceName} class. The examples are taken from the aws documentation at
 * http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonResourceNameTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testNameIsNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("name must not be null");
		AmazonResourceName.fromString(null);
	}

	@Test
	public void testWithoutArnQualifier() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("must have a arn qualifier at the beginning");
		AmazonResourceName.fromString("foo:aws:iam::123456789012:David");
	}

	@Test
	public void testWithoutAwsQualifier() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("must have a aws qualifier");
		AmazonResourceName.fromString("arn:axs:iam::123456789012:David");
	}

	@Test
	public void testDynamoDb() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:dynamodb:us-east-1:123456789012:table/books_table");
		Assert.assertEquals("dynamodb", resourceName.getService());
		Assert.assertEquals("us-east-1", resourceName.getRegion());
		Assert.assertEquals("123456789012", resourceName.getAccount());
		Assert.assertEquals("table", resourceName.getResourceType());
		Assert.assertEquals("books_table", resourceName.getResourceName());
	}

	@Test
	public void testElasticBeansTalk() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:elasticbeanstalk:us-east-1::solutionstack/32bit Amazon Linux running Tomcat 7");
		Assert.assertEquals("elasticbeanstalk", resourceName.getService());
		Assert.assertEquals("us-east-1", resourceName.getRegion());
		Assert.assertNull(resourceName.getAccount());
		Assert.assertEquals("solutionstack", resourceName.getResourceType());
		Assert.assertEquals("32bit Amazon Linux running Tomcat 7", resourceName.getResourceName());
	}

	@Test
	public void testIamService() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:iam::123456789012:server-certificate/ProdServerCert");
		Assert.assertEquals("iam", resourceName.getService());
		Assert.assertNull(resourceName.getRegion());
		Assert.assertEquals("123456789012", resourceName.getAccount());
		Assert.assertEquals("server-certificate", resourceName.getResourceType());
		Assert.assertEquals("ProdServerCert", resourceName.getResourceName());
	}

	@Test
	public void testRdsService() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:rds:us-west-2:123456789012:db:mysql-db");
		Assert.assertEquals("rds", resourceName.getService());
		Assert.assertEquals("us-west-2", resourceName.getRegion());
		Assert.assertEquals("123456789012", resourceName.getAccount());
		Assert.assertEquals("db", resourceName.getResourceType());
		Assert.assertEquals("mysql-db", resourceName.getResourceName());
	}

	@Test
	public void testRoute53Service() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:route53:::hostedzone/Z148QEXAMPLE8V");
		Assert.assertEquals("route53", resourceName.getService());
		Assert.assertNull(resourceName.getRegion());
		Assert.assertNull(resourceName.getAccount());
		Assert.assertEquals("hostedzone", resourceName.getResourceType());
		Assert.assertEquals("Z148QEXAMPLE8V", resourceName.getResourceName());
	}

	@Test
	public void testS3Service() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:s3:::my_corporate_bucket/Development/*");
		Assert.assertEquals("s3", resourceName.getService());
		Assert.assertNull(resourceName.getRegion());
		Assert.assertNull(resourceName.getAccount());
		Assert.assertEquals("my_corporate_bucket", resourceName.getResourceType());
		Assert.assertEquals("Development/*", resourceName.getResourceName());
	}

	@Test
	public void testSnsService() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:sns:us-east-1:123456789012:my_corporate_topic:02034b43-fefa-4e07-a5eb-3be56f8c54ce");
		Assert.assertEquals("sns", resourceName.getService());
		Assert.assertEquals("us-east-1", resourceName.getRegion());
		Assert.assertEquals("123456789012", resourceName.getAccount());
		Assert.assertEquals("my_corporate_topic", resourceName.getResourceType());
		Assert.assertEquals("02034b43-fefa-4e07-a5eb-3be56f8c54ce", resourceName.getResourceName());
	}

	@Test
	public void testSqsService() throws Exception {
		AmazonResourceName resourceName = AmazonResourceName.fromString("arn:aws:sqs:us-east-1:123456789012:queue1");
		Assert.assertEquals("sqs", resourceName.getService());
		Assert.assertEquals("us-east-1", resourceName.getRegion());
		Assert.assertEquals("123456789012", resourceName.getAccount());
		Assert.assertEquals("queue1", resourceName.getResourceType());
		Assert.assertNull(resourceName.getResourceName());
	}
}
