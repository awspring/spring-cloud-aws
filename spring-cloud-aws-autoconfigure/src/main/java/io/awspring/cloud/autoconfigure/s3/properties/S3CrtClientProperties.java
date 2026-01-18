/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3.properties;

import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;

/**
 * Properties related to {@link S3CrtAsyncClient}.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class S3CrtClientProperties {

	/**
	 * Sets the minimum part size for transfer parts. Decreasing the minimum part size causes multipart transfer to be
	 * split into a larger number of smaller parts. Setting this value too low has a negative effect on transfer speeds,
	 * causing extra latency and network communication for each part.
	 */
	@Nullable
	private Long minimumPartSizeInBytes;

	/**
	 * Configure the starting buffer size the client will use to buffer the parts downloaded from S3. Maintain a larger
	 * window to keep up a high download throughput; parts cannot download in parallel unless the window is large enough
	 * to hold multiple parts. Maintain a smaller window to limit the amount of data buffered in memory.
	 */
	@Nullable
	private Long initialReadBufferSizeInBytes;

	/**
	 * The target throughput for transfer requests. Higher value means more S3 connections will be opened. Whether the
	 * transfer manager can achieve the configured target throughput depends on various factors such as the network
	 * bandwidth of the environment and the configured `max-concurrency`.
	 */
	@Nullable
	private Double targetThroughputInGbps;

	/**
	 * Specifies the maximum number of S3 connections that should be established during transfer.
	 */
	@Nullable
	private Integer maxConcurrency;

	@Nullable
	public Double getTargetThroughputInGbps() {
		return this.targetThroughputInGbps;
	}

	public void setTargetThroughputInGbps(@Nullable Double targetThroughputInGbps) {
		this.targetThroughputInGbps = targetThroughputInGbps;
	}

	@Nullable
	public Integer getMaxConcurrency() {
		return this.maxConcurrency;
	}

	public void setMaxConcurrency(@Nullable Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	@Nullable
	public Long getMinimumPartSizeInBytes() {
		return this.minimumPartSizeInBytes;
	}

	public void setMinimumPartSizeInBytes(@Nullable Long minimumPartSizeInBytes) {
		this.minimumPartSizeInBytes = minimumPartSizeInBytes;
	}

	@Nullable
	public Long getInitialReadBufferSizeInBytes() {
		return initialReadBufferSizeInBytes;
	}

	public void setInitialReadBufferSizeInBytes(@Nullable Long initialReadBufferSizeInBytes) {
		this.initialReadBufferSizeInBytes = initialReadBufferSizeInBytes;
	}
}
