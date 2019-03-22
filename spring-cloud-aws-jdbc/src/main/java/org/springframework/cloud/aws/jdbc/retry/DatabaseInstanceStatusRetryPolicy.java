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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.util.Assert;

/**
 * {@link RetryPolicy} implementation that checks if it is useful to retry an operation
 * based on the database instance status. This class retrieves that database state and
 * verifies through the {@link InstanceStatus} enum operation if it is useful to retry the
 * operation. This class does not retrieve the status if there is no Throwable registered
 * to avoid any performance implication during normal operations.
 * <p>
 * This class should not be used alone because this would lead into a infinite retry,
 * because this class does not limit the amount of retries. Consider using this class
 * together with the {@link SqlRetryPolicy} which limits the maximum number of retries.
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class DatabaseInstanceStatusRetryPolicy implements RetryPolicy {

	/**
	 * DB-Instance attribute name used inside the {@link RetryContext} to store the
	 * database instance name during the retries.
	 */
	private static final String DB_INSTANCE_ATTRIBUTE_NAME = "DbInstanceIdentifier";

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DatabaseInstanceStatusRetryPolicy.class);

	/**
	 * Database instance identifier which should be checked.
	 */
	private final String dbInstanceIdentifier;

	/**
	 * {@link AmazonRDS} client used to query the Amazon RDS service.
	 */
	private final AmazonRDS amazonRDS;

	private ResourceIdResolver resourceIdResolver;

	/**
	 * Constructs this strategy implementation with it default and mandatory
	 * collaborators.
	 * @param amazonRDS - used to query the Amazon RDS service, must not be null
	 * @param dbInstanceIdentifier - database instance for which this class should check
	 * the state.
	 */
	public DatabaseInstanceStatusRetryPolicy(AmazonRDS amazonRDS,
			String dbInstanceIdentifier) {
		Assert.notNull(amazonRDS, "amazonRDS must not be null.");
		this.amazonRDS = amazonRDS;
		this.dbInstanceIdentifier = dbInstanceIdentifier;
	}

	/**
	 * Configures an option.
	 * {@link org.springframework.cloud.aws.core.env.ResourceIdResolver} to resolve
	 * logical name to physical name
	 * @param resourceIdResolver - the resourceIdResolver to be used, may be null
	 */
	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	/**
	 * Implementation that checks if there is an exception registered through
	 * {@link #registerThrowable(org.springframework.retry.RetryContext, Throwable)}.
	 * Returns <code>true</code> if there is no exception registered at all and verifies
	 * the database instance status if there is one registered.
	 * @param context - the retry context which may contain a registered exception
	 * @return <code>true</code> if there is no exception registered or if there is a
	 * retry useful which is verified by the {@link InstanceStatus} enum.
	 */
	@Override
	public boolean canRetry(RetryContext context) {
		// noinspection ThrowableResultOfMethodCallIgnored
		return (context.getLastThrowable() == null || isDatabaseAvailable(context));
	}

	@Override
	public RetryContext open(RetryContext parent) {
		RetryContextSupport context = new RetryContextSupport(parent);
		context.setAttribute(DB_INSTANCE_ATTRIBUTE_NAME, getDbInstanceIdentifier());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Starting RetryContext for database instance with identifier {}",
					getDbInstanceIdentifier());
		}
		return context;
	}

	@Override
	public void close(RetryContext context) {
		context.removeAttribute(DB_INSTANCE_ATTRIBUTE_NAME);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Closing RetryContext for database instance with identifier {}",
					getDbInstanceIdentifier());
		}
	}

	@Override
	public void registerThrowable(RetryContext context, Throwable throwable) {
		((RetryContextSupport) context).registerThrowable(throwable);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Registered Throwable of Type {} for RetryContext",
					throwable.getClass().getName());
		}
	}

	private boolean isDatabaseAvailable(RetryContext context) {
		DescribeDBInstancesResult describeDBInstancesResult;
		try {
			describeDBInstancesResult = this.amazonRDS.describeDBInstances(
					new DescribeDBInstancesRequest().withDBInstanceIdentifier(
							(String) context.getAttribute(DB_INSTANCE_ATTRIBUTE_NAME)));
		}
		catch (DBInstanceNotFoundException e) {
			LOGGER.warn(
					"Database Instance with name {} has been removed or is not configured correctly, no retry possible",
					getDbInstanceIdentifier());
			// Database has been deleted while operating, hence we can not retry
			return false;
		}

		if (describeDBInstancesResult.getDBInstances().size() == 1) {
			DBInstance dbInstance = describeDBInstancesResult.getDBInstances().get(0);
			InstanceStatus instanceStatus = InstanceStatus
					.fromDatabaseStatus(dbInstance.getDBInstanceStatus());
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Status of database to be retried is {}", instanceStatus);
			}
			return instanceStatus.isRetryable();
		}
		else {
			throw new IllegalStateException(
					"Multiple databases found for same identifier, this is likely an incompatibility with the Amazon SDK");
		}
	}

	private String getDbInstanceIdentifier() {
		return this.resourceIdResolver != null ? this.resourceIdResolver
				.resolveToPhysicalResourceId(this.dbInstanceIdentifier)
				: this.dbInstanceIdentifier;
	}

}
