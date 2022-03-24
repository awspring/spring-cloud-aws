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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = S3Properties.PREFIX)
public class S3Properties extends AwsClientProperties {

	/**
	 * The prefix used for S3 related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.s3";

	private Boolean accelerateModeEnabled;

	private Boolean checksumValidationEnabled;

	private Boolean chunkedEncodingEnabled;

	private Boolean dualstackEnabled;

	private Boolean pathStyleAccessEnabled;

	private Boolean useArnRegionEnabled;

	public Boolean getAccelerateModeEnabled() {
		return accelerateModeEnabled;
	}

	public void setAccelerateModeEnabled(Boolean accelerateModeEnabled) {
		this.accelerateModeEnabled = accelerateModeEnabled;
	}

	public Boolean getChecksumValidationEnabled() {
		return checksumValidationEnabled;
	}

	public void setChecksumValidationEnabled(Boolean checksumValidationEnabled) {
		this.checksumValidationEnabled = checksumValidationEnabled;
	}

	public Boolean getChunkedEncodingEnabled() {
		return chunkedEncodingEnabled;
	}

	public void setChunkedEncodingEnabled(Boolean chunkedEncodingEnabled) {
		this.chunkedEncodingEnabled = chunkedEncodingEnabled;
	}

	public Boolean getDualstackEnabled() {
		return dualstackEnabled;
	}

	public void setDualstackEnabled(Boolean dualstackEnabled) {
		this.dualstackEnabled = dualstackEnabled;
	}

	public Boolean getPathStyleAccessEnabled() {
		return pathStyleAccessEnabled;
	}

	public void setPathStyleAccessEnabled(Boolean pathStyleAccessEnabled) {
		this.pathStyleAccessEnabled = pathStyleAccessEnabled;
	}

	public Boolean getUseArnRegionEnabled() {
		return useArnRegionEnabled;
	}

	public void setUseArnRegionEnabled(Boolean useArnRegionEnabled) {
		this.useArnRegionEnabled = useArnRegionEnabled;
	}

}
