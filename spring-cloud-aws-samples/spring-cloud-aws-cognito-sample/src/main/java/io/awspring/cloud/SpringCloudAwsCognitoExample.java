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
package io.awspring.cloud;

import io.awspring.cloud.cognito.CognitoTemplate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;

@SpringBootApplication
public class SpringCloudAwsCognitoExample {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudAwsCognitoExample.class);
	private static final String USERNAME = "foo@bar.com";

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudAwsCognitoExample.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(CognitoTemplate cognitoTemplate) {
		return args -> {

			cognitoTemplate.createUser(USERNAME, getAttributes());
			LOGGER.info("User created, check your email");
			AdminInitiateAuthResponse authResponse = cognitoTemplate.login(USERNAME, "password");
			if (ChallengeNameType.NEW_PASSWORD_REQUIRED.equals(authResponse.challengeName())) {
				String session = authResponse.session();
				cognitoTemplate.setPermanentPassword(session, USERNAME, "superSecurePassword");
			}
			// your Access Token, Id Token and Refresh Token are stored here
			AuthenticationResultType authenticationResultType = authResponse.authenticationResult();
			LOGGER.info("Authentication result: {}", authenticationResultType);

			cognitoTemplate.resetPassword(USERNAME);
			LOGGER.info("Check your email for password reset instructions");
			cognitoTemplate.confirmResetPassword(USERNAME, "confirmationCode", "newSuperSecurePassword");
		};
	}

	private List<AttributeType> getAttributes() {
		return List.of(AttributeType.builder().name("email").value(USERNAME).build()
		// and all other attributes here
		);
	}
}
