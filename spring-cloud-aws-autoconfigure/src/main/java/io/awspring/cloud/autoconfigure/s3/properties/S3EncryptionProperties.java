/*
 * Copyright 2013-2024 the original author or authors.
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

/**
 * Properties to configure {@link software.amazon.encryption.s3.S3EncryptionClient}
 * @author Matej Nedic
 */
public class S3EncryptionProperties {

	private boolean autoGenerateKey = false;
	private int keyLength;
	private boolean enableLegacyUnauthenticatedModes = false;
	private boolean enableDelayedAuthenticationMode = false;
	private boolean enableMultipartPutObject = false;
	private S3EncryptionType type;

	public String getKmsId() {
		return kmsId;
	}

	public void setKmsId(String kmsId) {
		this.kmsId = kmsId;
	}

	private String kmsId;

	public boolean isEnableLegacyUnauthenticatedModes() {
		return enableLegacyUnauthenticatedModes;
	}

	public void setEnableLegacyUnauthenticatedModes(boolean enableLegacyUnauthenticatedModes) {
		this.enableLegacyUnauthenticatedModes = enableLegacyUnauthenticatedModes;
	}

	public boolean isEnableDelayedAuthenticationMode() {
		return enableDelayedAuthenticationMode;
	}

	public void setEnableDelayedAuthenticationMode(boolean enableDelayedAuthenticationMode) {
		this.enableDelayedAuthenticationMode = enableDelayedAuthenticationMode;
	}

	public boolean isEnableMultipartPutObject() {
		return enableMultipartPutObject;
	}

	public void setEnableMultipartPutObject(boolean enableMultipartPutObject) {
		this.enableMultipartPutObject = enableMultipartPutObject;
	}

	public S3EncryptionType getType() {
		return type;
	}

	public void setType(S3EncryptionType type) {
		this.type = type;
	}

	public boolean isAutoGenerateKey() {
		return autoGenerateKey;
	}

	public void setAutoGenerateKey(boolean autoGenerateKey) {
		this.autoGenerateKey = autoGenerateKey;
	}

	public int getKeyLength() {
		return keyLength;
	}

	public void setKeyLength(int keyLength) {
		this.keyLength = keyLength;
	}
}
