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

package org.springframework.cloud.aws.core.naming;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.aws.core.naming.AmazonResourceName.Builder;
import static org.springframework.cloud.aws.core.naming.AmazonResourceName.fromString;

/**
 * Test for {@link AmazonResourceName} class. The examples are taken from the aws
 * documentation at
 * https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonResourceNameTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testNameIsNull() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("name must not be null");
		fromString(null);
	}

	@Test
	public void testWithoutArnQualifier() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException
				.expectMessage("must have an arn qualifier at the beginning");
		fromString("foo:aws:iam::123456789012:David");
	}

	@Test
	public void testWithoutAwsQualifier() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("must have a valid partition name");
		fromString("arn:axs:iam::123456789012:David");
	}

	@Test
	public void testWithCustomPartitionName() {
		AmazonResourceName resourceName = fromString(
				"arn:aws-cn:iam::123456789012:David");
		assertThat(resourceName.getPartition()).isEqualTo("aws-cn");
	}

	@Test
	public void testDynamoDb() {
		String arn = "arn:aws:dynamodb:us-east-1:123456789012:table/books_table";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("dynamodb");
		assertThat(resourceName.getRegion()).isEqualTo("us-east-1");
		assertThat(resourceName.getAccount()).isEqualTo("123456789012");
		assertThat(resourceName.getResourceType()).isEqualTo("table");
		assertThat(resourceName.getResourceName()).isEqualTo("books_table");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testDynamoDbBuilder() {
		Builder builder = new Builder();
		builder.withService("dynamodb");
		builder.withRegion(Region.getRegion(Regions.US_EAST_1));
		builder.withAccount("123456789012");
		builder.withResourceType("table");
		builder.withResourceName("books_table");
		builder.withResourceTypeDelimiter("/");
		assertThat(builder.build().toString())
				.isEqualTo("arn:aws:dynamodb:us-east-1:123456789012:table/books_table");
	}

	@Test
	public void testElasticBeansTalkBuilder() {
		Builder builder = new Builder();
		builder.withService("elasticbeanstalk");
		builder.withRegion(Region.getRegion(Regions.US_EAST_1));
		builder.withResourceType("solutionstack");
		builder.withResourceName("32bit Amazon Linux running Tomcat 7");
		builder.withResourceTypeDelimiter("/");
		assertThat(builder.build().toString()).isEqualTo(
				"arn:aws:elasticbeanstalk:us-east-1::solutionstack/32bit Amazon Linux running Tomcat 7");
	}

	@Test
	public void testElasticBeansTalk() {
		String arn = "arn:aws:elasticbeanstalk:us-east-1::solutionstack/32bit Amazon Linux running Tomcat 7";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("elasticbeanstalk");
		assertThat(resourceName.getRegion()).isEqualTo("us-east-1");
		assertThat(resourceName.getAccount()).isNull();
		assertThat(resourceName.getResourceType()).isEqualTo("solutionstack");
		assertThat(resourceName.getResourceName())
				.isEqualTo("32bit Amazon Linux running Tomcat 7");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testIamService() {
		String arn = "arn:aws:iam::123456789012:server-certificate/ProdServerCert";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("iam");
		assertThat(resourceName.getRegion()).isNull();
		assertThat(resourceName.getAccount()).isEqualTo("123456789012");
		assertThat(resourceName.getResourceType()).isEqualTo("server-certificate");
		assertThat(resourceName.getResourceName()).isEqualTo("ProdServerCert");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testRdsService() {
		String arn = "arn:aws:rds:us-west-2:123456789012:db:mysql-db";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("rds");
		assertThat(resourceName.getRegion()).isEqualTo("us-west-2");
		assertThat(resourceName.getAccount()).isEqualTo("123456789012");
		assertThat(resourceName.getResourceType()).isEqualTo("db");
		assertThat(resourceName.getResourceName()).isEqualTo("mysql-db");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testRoute53Service() {
		String arn = "arn:aws:route53:::hostedzone/Z148QEXAMPLE8V";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("route53");
		assertThat(resourceName.getRegion()).isNull();
		assertThat(resourceName.getAccount()).isNull();
		assertThat(resourceName.getResourceType()).isEqualTo("hostedzone");
		assertThat(resourceName.getResourceName()).isEqualTo("Z148QEXAMPLE8V");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testS3Service() {
		String arn = "arn:aws:s3:::my_corporate_bucket/Development/*";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("s3");
		assertThat(resourceName.getRegion()).isNull();
		assertThat(resourceName.getAccount()).isNull();
		assertThat(resourceName.getResourceType()).isEqualTo("my_corporate_bucket");
		assertThat(resourceName.getResourceName()).isEqualTo("Development/*");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testSnsService() {
		String arn = "arn:aws:sns:us-east-1:123456789012:my_corporate_topic:02034b43-fefa-4e07-a5eb-3be56f8c54ce";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("sns");
		assertThat(resourceName.getRegion()).isEqualTo("us-east-1");
		assertThat(resourceName.getAccount()).isEqualTo("123456789012");
		assertThat(resourceName.getResourceType()).isEqualTo("my_corporate_topic");
		assertThat(resourceName.getResourceName())
				.isEqualTo("02034b43-fefa-4e07-a5eb-3be56f8c54ce");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testSqsService() {
		String arn = "arn:aws:sqs:us-east-1:123456789012:queue1";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("sqs");
		assertThat(resourceName.getRegion()).isEqualTo("us-east-1");
		assertThat(resourceName.getAccount()).isEqualTo("123456789012");
		assertThat(resourceName.getResourceType()).isEqualTo("queue1");
		assertThat(resourceName.getResourceName()).isNull();
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

	@Test
	public void testGovCloudAwsQualifier() {
		String arn = "arn:aws-us-gov:sns:us-gov-east-1:123456789012:my_corporate_topic:02034b43-fefa-4e07-a5eb-3be56f8c54ce";
		AmazonResourceName resourceName = fromString(arn);
		assertThat(resourceName.getService()).isEqualTo("sns");
		assertThat(resourceName.getRegion()).isEqualTo("us-gov-east-1");
		assertThat(resourceName.getAccount()).isEqualTo("123456789012");
		assertThat(resourceName.getResourceType()).isEqualTo("my_corporate_topic");
		assertThat(resourceName.getResourceName())
				.isEqualTo("02034b43-fefa-4e07-a5eb-3be56f8c54ce");
		assertThat(resourceName.toString()).isEqualTo(arn);
	}

}
