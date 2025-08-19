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
package io.awspring.cloud.cognito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for Cognito operations.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */
public class CognitoUtils {

	private CognitoUtils() {
	}

	// https://docs.aws.amazon.com/cognito/latest/developerguide/signing-up-users-in-your-app.html#cognito-user-pools-computing-secret-hash
	public static String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
		final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
		SecretKeySpec signingKey = new SecretKeySpec(userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
				HMAC_SHA256_ALGORITHM);
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
			mac.init(signingKey);
			mac.update(userName.getBytes(StandardCharsets.UTF_8));
			byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(rawHmac);
		}
		catch (Exception e) {
			throw new RuntimeException("Error while calculating secret hash for " + userName);
		}
	}
}
