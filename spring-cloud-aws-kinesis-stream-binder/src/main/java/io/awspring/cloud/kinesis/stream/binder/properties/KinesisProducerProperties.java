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

/**
 * The Kinesis-specific producer binding configuration properties.
 *
 * @author Peter Oates
 * @author Jacob Severson
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class KinesisProducerProperties {

	/**
	 * Whether message handler produces in sync mode.
	 */
	private boolean sync;

	/**
	 * Timeout in milliseconds to wait for future completion in sync mode.
	 */
	private long sendTimeout = 10000;

	/**
	 * Whether to embed headers into Kinesis record.
	 */
	private boolean embedHeaders = true;

	/**
	 * The bean name of a MessageChannel to which successful send results should be sent. Works only for async mode.
	 */
	private String recordMetadataChannel;

	/**
	 * Maximum records in flight for handling backpressure. No backpressure by default. When backpressure handling is
	 * enabled and number of records in flight exceeds the threshold, a 'KplBackpressureException' would be thrown.
	 */
	private long kplBackPressureThreshold;

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	public boolean isSync() {
		return this.sync;
	}

	public long getSendTimeout() {
		return this.sendTimeout;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public boolean isEmbedHeaders() {
		return this.embedHeaders;
	}

	public void setEmbedHeaders(boolean embedHeaders) {
		this.embedHeaders = embedHeaders;
	}

	public String getRecordMetadataChannel() {
		return this.recordMetadataChannel;
	}

	public void setRecordMetadataChannel(String recordMetadataChannel) {
		this.recordMetadataChannel = recordMetadataChannel;
	}

	public long getKplBackPressureThreshold() {
		return this.kplBackPressureThreshold;
	}

	public void setKplBackPressureThreshold(long kplBackPressureThreshold) {
		this.kplBackPressureThreshold = kplBackPressureThreshold;
	}

}
