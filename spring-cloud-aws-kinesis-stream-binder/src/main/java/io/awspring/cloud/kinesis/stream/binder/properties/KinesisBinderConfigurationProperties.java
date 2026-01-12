/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.stream.binder.properties;

import io.awspring.cloud.dynamodb.DynamoDbLockRepository;
import io.awspring.cloud.dynamodb.DynamoDbMetadataStore;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;

/**
 * The Kinesis Binder specific configuration properties.
 *
 * @author Peter Oates
 * @author Artem Bilan
 * @author Jacob Severson
 * @author Sergiu Pantiru
 * @author Arnaud Lecollaire
 * @author Asiel Caballero
 *
 * @since 4.0
 */
@ConfigurationProperties(prefix = "spring.cloud.stream.kinesis.binder")
public class KinesisBinderConfigurationProperties {

	private String[] headers = new String[] {};

	private int describeStreamBackoff = 1000;

	private int describeStreamRetries = 50;

	private boolean autoCreateStream = true;

	private boolean autoAddShards = false;

	private int minShardCount = 1;

	/**
	 * Enables the usage of Amazon KCL/KPL libraries for all message consumption and production.
	 */
	private boolean kplKclEnabled;

	/**
	 * Enable Micrometer observation registry across all the bindings in the binder.
	 */
	private boolean enableObservation;

	private final Checkpoint checkpoint = new Checkpoint();

	private final Locks locks = new Locks();

	public String[] getHeaders() {
		return this.headers;
	}

	public void setHeaders(String... headers) {
		this.headers = headers;
	}

	public int getDescribeStreamBackoff() {
		return this.describeStreamBackoff;
	}

	public void setDescribeStreamBackoff(int describeStreamBackoff) {
		this.describeStreamBackoff = describeStreamBackoff;
	}

	public int getDescribeStreamRetries() {
		return this.describeStreamRetries;
	}

	public void setDescribeStreamRetries(int describeStreamRetries) {
		this.describeStreamRetries = describeStreamRetries;
	}

	public boolean isAutoAddShards() {
		return this.autoAddShards;
	}

	public void setAutoAddShards(boolean autoAddShards) {
		this.autoAddShards = autoAddShards;
	}

	public int getMinShardCount() {
		return this.minShardCount;
	}

	public void setMinShardCount(int minShardCount) {
		this.minShardCount = minShardCount;
	}

	public Checkpoint getCheckpoint() {
		return this.checkpoint;
	}

	public Locks getLocks() {
		return this.locks;
	}

	public boolean isAutoCreateStream() {
		return this.autoCreateStream;
	}

	public void setAutoCreateStream(boolean autoCreateStream) {
		this.autoCreateStream = autoCreateStream;
	}

	public boolean isKplKclEnabled() {
		return this.kplKclEnabled;
	}

	public void setKplKclEnabled(boolean kplKclEnabled) {
		this.kplKclEnabled = kplKclEnabled;
	}

	public boolean isEnableObservation() {
		return this.enableObservation;
	}

	public void setEnableObservation(boolean enableObservation) {
		this.enableObservation = enableObservation;
	}

	/**
	 * The checkpoint DynamoDB table configuration properties.
	 */
	public static class Checkpoint {

		private String table = DynamoDbMetadataStore.DEFAULT_TABLE_NAME;

		private BillingMode billingMode = BillingMode.PAY_PER_REQUEST;

		private long readCapacity = 1L;

		private long writeCapacity = 1L;

		private int createDelay = 1;

		private int createRetries = 25;

		private Integer timeToLive;

		public String getTable() {
			return this.table;
		}

		public void setTable(String table) {
			this.table = table;
		}

		public BillingMode getBillingMode() {
			return billingMode;
		}

		public void setBillingMode(BillingMode billingMode) {
			this.billingMode = billingMode;
		}

		public long getReadCapacity() {
			return this.readCapacity;
		}

		public void setReadCapacity(long readCapacity) {
			this.readCapacity = readCapacity;
		}

		public long getWriteCapacity() {
			return this.writeCapacity;
		}

		public void setWriteCapacity(long writeCapacity) {
			this.writeCapacity = writeCapacity;
		}

		public int getCreateDelay() {
			return this.createDelay;
		}

		public void setCreateDelay(int createDelay) {
			this.createDelay = createDelay;
		}

		public int getCreateRetries() {
			return this.createRetries;
		}

		public void setCreateRetries(int createRetries) {
			this.createRetries = createRetries;
		}

		public Integer getTimeToLive() {
			return this.timeToLive;
		}

		public void setTimeToLive(Integer timeToLive) {
			this.timeToLive = timeToLive;
		}

	}

	/**
	 * The locks DynamoDB table configuration properties.
	 */
	public static class Locks {

		private String table = DynamoDbLockRepository.DEFAULT_TABLE_NAME;

		private BillingMode billingMode = BillingMode.PAY_PER_REQUEST;

		private long readCapacity = 1L;

		private long writeCapacity = 1L;

		private Duration refreshPeriod = Duration.ofSeconds(1);

		private Duration leaseDuration = Duration.ofSeconds(60);

		public String getTable() {
			return this.table;
		}

		public void setTable(String table) {
			this.table = table;
		}

		public BillingMode getBillingMode() {
			return billingMode;
		}

		public void setBillingMode(BillingMode billingMode) {
			this.billingMode = billingMode;
		}

		public long getReadCapacity() {
			return this.readCapacity;
		}

		public void setReadCapacity(long readCapacity) {
			this.readCapacity = readCapacity;
		}

		public long getWriteCapacity() {
			return this.writeCapacity;
		}

		public void setWriteCapacity(long writeCapacity) {
			this.writeCapacity = writeCapacity;
		}

		public Duration getRefreshPeriod() {
			return this.refreshPeriod;
		}

		public void setRefreshPeriod(Duration refreshPeriod) {
			this.refreshPeriod = refreshPeriod;
		}

		public Duration getLeaseDuration() {
			return this.leaseDuration;
		}

		public void setLeaseDuration(Duration leaseDuration) {
			this.leaseDuration = leaseDuration;
		}

	}

}
