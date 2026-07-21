/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.autoconfigure.sqs;

import org.jspecify.annotations.Nullable;

/**
 * Properties for configuring the Amazon SQS Extended Client Library, which offloads large message payloads to Amazon
 * S3. These mirror the options of {@code com.amazon.sqs.javamessaging.ExtendedAsyncClientConfiguration}.
 *
 * @author Matej Nedic
 */
public class SqsExtendedClientProperties {

	/**
	 * Name of the S3 bucket used to store large message payloads. Required to enable payload offloading.
	 */
	@Nullable
	private String bucket;

	/**
	 * Whether to delete the payload object from S3 when the corresponding message is deleted from the queue.
	 */
	private boolean cleanupS3Payload = true;

	/**
	 * Whether to use the legacy reserved message attribute name ({@code SQSLargePayloadSize}) when sending
	 * large-payload messages. Set to {@code false} to use the current reserved attribute name.
	 */
	private boolean useLegacyReservedAttributeName = true;

	/**
	 * Whether messages whose payload is not found in S3 should be silently deleted from the queue instead of failing.
	 */
	private boolean ignorePayloadNotFound = false;

	/**
	 * Prefix prepended to the S3 keys under which message payloads are stored.
	 */
	private String s3KeyPrefix = "";

	@Nullable
	public String getBucket() {
		return this.bucket;
	}

	public void setBucket(@Nullable String bucket) {
		this.bucket = bucket;
	}

	public boolean isCleanupS3Payload() {
		return this.cleanupS3Payload;
	}

	public void setCleanupS3Payload(boolean cleanupS3Payload) {
		this.cleanupS3Payload = cleanupS3Payload;
	}

	public boolean isUseLegacyReservedAttributeName() {
		return this.useLegacyReservedAttributeName;
	}

	public void setUseLegacyReservedAttributeName(boolean useLegacyReservedAttributeName) {
		this.useLegacyReservedAttributeName = useLegacyReservedAttributeName;
	}

	public boolean isIgnorePayloadNotFound() {
		return this.ignorePayloadNotFound;
	}

	public void setIgnorePayloadNotFound(boolean ignorePayloadNotFound) {
		this.ignorePayloadNotFound = ignorePayloadNotFound;
	}

	public String getS3KeyPrefix() {
		return this.s3KeyPrefix;
	}

	public void setS3KeyPrefix(String s3KeyPrefix) {
		this.s3KeyPrefix = s3KeyPrefix;
	}

}
