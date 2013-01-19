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

package org.elasticspring.jdbc.retry;


import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.elasticspring.jdbc.rds.InstanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;

/**
 *
 */
public class DatabaseInstanceStatusRetryPolicy implements RetryPolicy {

	private static final String DB_INSTANCE_ATTRIBUTE_NAME = "DbInstanceIdentifier";
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInstanceStatusRetryPolicy.class);

	private final String dbInstanceIdentifier;
	private final AmazonRDS amazonRDS;

	public DatabaseInstanceStatusRetryPolicy(AmazonRDS amazonRDS, String dbInstanceIdentifier) {
		this.amazonRDS = amazonRDS;
		this.dbInstanceIdentifier = dbInstanceIdentifier;
	}

	@Override
	public boolean canRetry(RetryContext context) {
		//noinspection ThrowableResultOfMethodCallIgnored
		return (context.getLastThrowable() == null || isDatabaseAvailable(context));
	}

	private boolean isDatabaseAvailable(RetryContext context) {
		DescribeDBInstancesResult describeDBInstancesResult;
		try {
			describeDBInstancesResult = this.amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier((String) context.getAttribute(DB_INSTANCE_ATTRIBUTE_NAME)));
		} catch (DBInstanceNotFoundException e) {
			LOGGER.warn("Database Instance with name {} has been removed or is not configured correctly, no retry possible", this.dbInstanceIdentifier);
			//Database has been deleted while operating, hence we can not retry
			return false;
		}

		if (describeDBInstancesResult.getDBInstances().size() == 1) {
			DBInstance dbInstance = describeDBInstancesResult.getDBInstances().get(0);
			InstanceStatus instanceStatus = InstanceStatus.valueOf(dbInstance.getDBInstanceStatus().toUpperCase());
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Status of database to be retried is {}", instanceStatus);
			}
			return instanceStatus.isRetryable();
		} else {
			throw new IllegalStateException("Multiple data bases found for same identifier, this is likely an incompatibility with the Amazon SDK");
		}
	}

	@Override
	public RetryContext open(RetryContext parent) {
		RetryContextSupport context = new RetryContextSupport(parent);
		context.setAttribute(DB_INSTANCE_ATTRIBUTE_NAME, this.dbInstanceIdentifier);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Starting RetryContext for database instance with identifier {}", this.dbInstanceIdentifier);
		}
		return context;
	}

	@Override
	public void close(RetryContext context) {
		context.removeAttribute(DB_INSTANCE_ATTRIBUTE_NAME);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Closing RetryContext for database instance with identifier {}", this.dbInstanceIdentifier);
		}
	}

	@Override
	public void registerThrowable(RetryContext context, Throwable throwable) {
		((RetryContextSupport) context).registerThrowable(throwable);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Registered Throwable of Type {} for RetryContext", throwable.getClass().getName());
		}
	}
}
