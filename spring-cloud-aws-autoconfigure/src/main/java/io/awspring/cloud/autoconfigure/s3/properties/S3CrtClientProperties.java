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

import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;

/**
 * Properties related to {@link S3CrtAsyncClient}.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class S3CrtClientProperties {
	@Nullable
	private Long minimumPartSizeInBytes;
	@Nullable
	private Long initialReadBufferSizeInBytes;
	@Nullable
	private Double targetThroughputInGbps;

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
