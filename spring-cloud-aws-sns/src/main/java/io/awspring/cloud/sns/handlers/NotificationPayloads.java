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
package io.awspring.cloud.sns.handlers;

import org.springframework.lang.Nullable;

/**
 * represent the AWS SNS Message payload data.
 * @author kazaff
 */
public class NotificationPayloads {

	private String type;

	private String messageId;

	@Nullable
	private String token;

	private String topicArn;

	@Nullable
	private String subject;

	private String message;

	@Nullable
	private String subscribeUrl;

	@Nullable
	private String unsubscribeUrl;

	private String timestamp;

	private String signatureVersion;

	private String signature;

	private String signingCertUrl;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Nullable
	public String getToken() {
		return token;
	}

	public void setToken(@Nullable String token) {
		this.token = token;
	}

	public String getTopicArn() {
		return topicArn;
	}

	public void setTopicArn(String topicArn) {
		this.topicArn = topicArn;
	}

	@Nullable
	public String getSubject() {
		return subject;
	}

	public void setSubject(@Nullable String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Nullable
	public String getSubscribeUrl() {
		return subscribeUrl;
	}

	public void setSubscribeUrl(@Nullable String subscribeUrl) {
		this.subscribeUrl = subscribeUrl;
	}

	@Nullable
	public String getUnsubscribeUrl() {
		return unsubscribeUrl;
	}

	public void setUnsubscribeUrl(@Nullable String unsubscribeUrl) {
		this.unsubscribeUrl = unsubscribeUrl;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getSignatureVersion() {
		return signatureVersion;
	}

	public void setSignatureVersion(String signatureVersion) {
		this.signatureVersion = signatureVersion;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getSigningCertUrl() {
		return signingCertUrl;
	}

	public void setSigningCertUrl(String signingCertUrl) {
		this.signingCertUrl = signingCertUrl;
	}
}
