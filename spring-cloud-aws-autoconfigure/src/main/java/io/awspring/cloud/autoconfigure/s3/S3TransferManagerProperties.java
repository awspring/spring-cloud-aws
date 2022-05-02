/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.lang.Nullable;

/**
 * Properties related to AWS S3 TransferManager.
 *
 * @author Anton Perez
 */
public class S3TransferManagerProperties extends AwsClientProperties {
	@Nullable
	private Double targetThroughputInGbps;

	@Nullable
	private Integer maxConcurrency;

	@Nullable
	private Long minimumPartSizeInBytes;

	private S3UploadDirectoryProperties uploadDirectory;

	@Nullable
	public Double getTargetThroughputInGbps() {
		return targetThroughputInGbps;
	}

	public void setTargetThroughputInGbps(@Nullable Double targetThroughputInGbps) {
		this.targetThroughputInGbps = targetThroughputInGbps;
	}

	public S3UploadDirectoryProperties getUploadDirectory() {
		return uploadDirectory;
	}

	public void setUploadDirectory(S3UploadDirectoryProperties uploadDirectory) {
		this.uploadDirectory = uploadDirectory;
	}

	@Nullable
	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(@Nullable Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	@Nullable
	public Long getMinimumPartSizeInBytes() {
		return minimumPartSizeInBytes;
	}

	public void setMinimumPartSizeInBytes(@Nullable Long minimumPartSizeInBytes) {
		this.minimumPartSizeInBytes = minimumPartSizeInBytes;
	}
}
