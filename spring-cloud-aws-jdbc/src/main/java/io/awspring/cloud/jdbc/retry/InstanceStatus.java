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

import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;

/**
 * Enumeration that holds all possible Amazon RDS database instance states. Used by client
 * code to check if the database is available at all, and to verify if a retry is useful.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public enum InstanceStatus {

	/**
	 * Database state that is available during creation of the database. This is typically
	 * the case if the database is started at the same time like the application is
	 * bootstrapped. In this case a retry will succeed.
	 */
	@RuntimeUse
	CREATING(true, false),

	/**
	 * Database state that defines that the database is available. The database can be
	 * used at all, also a retry for a temporary problem (like a dead-lock) is likely to
	 * succeed if called twice.
	 */
	@RuntimeUse
	AVAILABLE(true, true),

	/**
	 * Database state which defines that the database instance is rebooting. This can
	 * happen during a user initiated reboot. A second attempt is likely to succeed
	 * especially in the combination with a replicated Multi A/Z instance.
	 */
	@RuntimeUse
	REBOOTING(true, false),

	/**
	 * Database state which defines that the database instance is modifying. The database
	 * is still available and usable, but a modification is in progress. This can be the
	 * case if an read-replica instance is added to the database itself.
	 */
	@RuntimeUse
	MODIFYING(true, true),

	/**
	 * Database state which defines that the database is deleting. Thus it is assumed that
	 * is is deleted.
	 */
	@RuntimeUse
	DELETING(false, false),

	/**
	 * Unknown status, which assumes the database is not available and not retryable.
	 */
	@RuntimeUse
	UNKNOWN(false, false);

	/**
	 * boolean flag indicating if a second attempt to execute the operation will succeed.
	 */
	private final boolean retryable;

	/**
	 * boolean flag indicating if the database is available from the management.
	 * perspective of Amazon RDS
	 */
	private final boolean available;

	InstanceStatus(boolean retryable, boolean available) {
		this.retryable = retryable;
		this.available = available;
	}

	static InstanceStatus fromDatabaseStatus(String databaseStatus) {
		for (InstanceStatus instanceStatus : values()) {
			if (instanceStatus.name().equalsIgnoreCase(databaseStatus)) {
				return instanceStatus;
			}
		}
		return UNKNOWN;
	}

	/**
	 * Operation that returns <code>true</code> if a second call will likely succeed. This
	 * is typically the case if the database is down for temporary reasons like a reboot
	 * after a configuration change, or a multi a/z failover until the DNS record is
	 * updated. Clients should wait some time (e.g. up until 3 minutes) before retrying
	 * their last database operation.
	 * @return <code>true</code> if the operation is retryable, <code>false</code>
	 * otherwise
	 */
	public boolean isRetryable() {
		return this.retryable;
	}

	/**
	 * Operation that returns <code>true</code> if the database is available from the
	 * Amazon RDS perspective. That does not necessarily mean that there is a connection
	 * possible to that database itself. Due to wrong/missing rule in the security
	 * credentials it might be possible that a database is available but not connectible
	 * through the JDBC protocol.
	 * @return <code>true</code> if the database is available, <code>false</code>
	 * otherwise
	 */
	public boolean isAvailable() {
		return this.available;
	}

}
