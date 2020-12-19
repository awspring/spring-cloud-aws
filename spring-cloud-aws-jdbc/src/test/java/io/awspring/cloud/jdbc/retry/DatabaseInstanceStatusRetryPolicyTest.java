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

package org.springframework.cloud.aws.jdbc.retry;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.context.RetryContextSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test class for {@link DatabaseInstanceStatusRetryPolicy}.
 *
 * @author Agim Emruli
 */
@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
class DatabaseInstanceStatusRetryPolicyTest {

	@Test
	void canRetry_retryPossibleDueToAvailableDatabase_returnsTrue() throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(amazonRDS, "test");
		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
				.thenReturn(new DescribeDBInstancesResult()
						.withDBInstances(new DBInstance().withDBInstanceStatus("available")));

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext, new TransientDataAccessResourceException("not available"));

		// Assert
		assertThat(policy.canRetry(retryContext)).isTrue();
		policy.close(retryContext);
	}

	@Test
	void canRetry_withResourceIdResolver_returnsTrue() throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(amazonRDS, "foo");
		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
				.thenReturn(new DescribeDBInstancesResult()
						.withDBInstances(new DBInstance().withDBInstanceStatus("available")));

		when(resourceIdResolver.resolveToPhysicalResourceId("foo")).thenReturn("test");

		policy.setResourceIdResolver(resourceIdResolver);

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext, new TransientDataAccessResourceException("not available"));

		// Assert
		assertThat(policy.canRetry(retryContext)).isTrue();
		policy.close(retryContext);
	}

	@Test
	void canRetry_retryNotPossibleDueToNoDatabase_returnsFalse() throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(amazonRDS, "test");

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
				.thenThrow(new DBInstanceNotFoundException("test"));

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext, new TransientDataAccessResourceException("not available"));

		// Assert
		assertThat(policy.canRetry(retryContext)).isFalse();
		policy.close(retryContext);
	}

	@Test
	void canRetry_multipleDatabasesFoundForInstanceIdentifier_reportsException() throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(amazonRDS, "test");

		DescribeDBInstancesResult describeDBInstancesResult = new DescribeDBInstancesResult()
				.withDBInstances(new DBInstance(), new DBInstance());
		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
				.thenReturn(describeDBInstancesResult);

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext, new TransientDataAccessResourceException("not available"));

		// Assert

		assertThatThrownBy(() -> policy.canRetry(retryContext)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Multiple databases found for same identifier");
	}

	@Test
	void canRetry_noExceptionRegistered_returnsTrue() throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(amazonRDS, "test");

		RetryContext retryContext = new RetryContextSupport(null);

		// Act
		policy.open(retryContext);

		// Assert
		assertThat(policy.canRetry(retryContext)).isTrue();
	}

}
