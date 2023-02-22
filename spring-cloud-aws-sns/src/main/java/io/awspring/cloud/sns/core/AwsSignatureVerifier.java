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
package io.awspring.cloud.sns.core;

import io.awspring.cloud.sns.handlers.NotificationPayloads;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Check that the request is from SNS (has the well-known header). Checks that the Signing Cert URL is hosted at
 * amazonaws.com for SNS. Retrieves the signing certificate from the URL. Checks that the signing certificate is valid
 * (within validity dates). Builds the string-to-sign and verifies it against the signing certificate. Verifies the
 * signature against that string-to-sign. Checks that the timestamp is no more than {@link maxLifeTimeOfMessage} seconds
 * prior.
 *
 * @author Dino Chiesa (https://github.com/DinoChiesa/Apigee-Java-AWS-SNS-Verifier)
 * @author kazaff
 */
public class AwsSignatureVerifier {

	private long maxLifeTimeOfMessage;

	public AwsSignatureVerifier(long maxLifeTimeOfMessage) {
		this.maxLifeTimeOfMessage = maxLifeTimeOfMessage;
	}

	public boolean verify(NotificationPayloads payloads, HttpServletRequest httpServletRequest) {

		if (!isMessageHeaderValid(httpServletRequest)) {
			return false;
		}

		if (!isMessageBodyValid(payloads)) {
			return false;
		}

		if (maxLifeTimeOfMessage != 0L) {
			Instant expiry = Instant.parse(payloads.getTimestamp()).plusSeconds(maxLifeTimeOfMessage);
			Instant now = Instant.now();
			long secondsRemaining = now.until(expiry, ChronoUnit.SECONDS);
			if (secondsRemaining <= 0L) {
				return false;
			}
		}

		return isMessageSignatureValid(payloads);
	}

	private static boolean isMessageHeaderValid(HttpServletRequest httpServletRequest) {
		String content = httpServletRequest.getHeader("x-amz-sns-message-type");
		if (content == null || content.equals("")) {
			return false;
		}

		content = httpServletRequest.getHeader("x-amz-sns-message-id");
		if (content == null || content.equals("")) {
			return false;
		}

		content = httpServletRequest.getHeader("x-amz-sns-topic-arn");
		if (content == null || content.equals("")) {
			return false;
		}

		return true;
	}

	private static boolean isMessageBodyValid(NotificationPayloads payloads) {
		if (payloads.getMessage() == null || payloads.getMessageId() == null || payloads.getTimestamp() == null
				|| payloads.getTopicArn() == null || payloads.getType() == null || payloads.getSigningCertUrl() == null
				|| payloads.getSignatureVersion() == null || payloads.getSignature() == null) {
			return false;
		}
		return true;
	}

	private static boolean isMessageSignatureValid(NotificationPayloads payloads) {
		try {
			String certUri = payloads.getSigningCertUrl();
			verifyCertificateURL(certUri);
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(CertCache.getCert(certUri).getPublicKey());
			sig.update(getMessageBytesToSign(payloads));
			return sig.verify(Base64.getDecoder().decode(payloads.getSignature()));
		}
		catch (Exception e) {
			throw new SecurityException("Verify failed", e);
		}
	}

	private static void verifyCertificateURL(String signingCertUri) {
		URI certUri = URI.create(signingCertUri);
		if (!"https".equals(certUri.getScheme())) {
			throw new SecurityException("SigningCertURL was not using HTTPS: " + certUri.toString());
		}

		String hostname = certUri.getHost();
		if (!hostname.startsWith("sns") || !hostname.endsWith("amazonaws.com")) {
			throw new SecurityException("SigningCertUrl appears to be invalid.");
		}
	}

	private static byte[] getMessageBytesToSign(NotificationPayloads payloads) {
		String type = payloads.getType();
		if ("Notification".equals(type)) {
			return StringToSign.forNotification(payloads).getBytes(StandardCharsets.UTF_8);
		}

		if ("SubscriptionConfirmation".equals(type) || "UnsubscribeConfirmation".equals(type)) {
			return StringToSign.forSubscription(payloads).getBytes(StandardCharsets.UTF_8);
		}

		return null;
	}

	private static class StringToSign {
		public static String forNotification(NotificationPayloads payloads) {
			String stringToSign = "Message\n" + payloads.getMessage() + "\n" + "MessageId\n" + payloads.getMessageId()
					+ "\n";
			if (payloads.getSubject() != null) {
				stringToSign += "Subject\n" + payloads.getSubject() + "\n";
			}
			stringToSign += "Timestamp\n" + payloads.getTimestamp() + "\n" + "TopicArn\n" + payloads.getTopicArn()
					+ "\n" + "Type\n" + payloads.getType() + "\n";
			return stringToSign;
		}

		public static String forSubscription(NotificationPayloads payloads) {
			String stringToSign = "Message\n" + payloads.getMessage() + "\n" + "MessageId\n" + payloads.getMessageId()
					+ "\n" + "SubscribeURL\n" + payloads.getSubscribeUrl() + "\n" + "Timestamp\n"
					+ payloads.getTimestamp() + "\n" + "Token\n" + payloads.getToken() + "\n" + "TopicArn\n"
					+ payloads.getTopicArn() + "\n" + "Type\n" + payloads.getType() + "\n";
			return stringToSign;
		}
	}
}
