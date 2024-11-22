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
package io.awspring.cloud.cognito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUserGlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest;

/**
 * Tests for {@link CognitoTemplate}.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */
class CognitoTemplateTest {

	private static final String USERNAME = "foo@bar.com";
	private static final String PASSWORD = "password";
	private final CognitoIdentityProviderClient cognitoIdentityProviderClient = mock(
			CognitoIdentityProviderClient.class);

	private final CognitoTemplate cognitoTemplate = new CognitoTemplate(cognitoIdentityProviderClient, "clientId",
			"userPoolId", "clientSecret");

	@Test
	void createUser() {
		AdminCreateUserRequest request = AdminCreateUserRequest.builder().userPoolId("userPoolId").username(USERNAME)
				.temporaryPassword(PASSWORD).userAttributes(createAttributes()).build();
		cognitoTemplate.createUser(USERNAME, PASSWORD, createAttributes());

		verify(cognitoIdentityProviderClient).adminCreateUser(request);
	}

	@Test
	void login() {
		AdminInitiateAuthRequest initiateAuthRequest = AdminInitiateAuthRequest.builder().userPoolId("userPoolId")
				.clientId("clientId").authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
				.authParameters(resolveAuthParameters()).build();

		cognitoTemplate.login(USERNAME, PASSWORD);

		verify(cognitoIdentityProviderClient).adminInitiateAuth(initiateAuthRequest);
	}

	@Test
	void resetPassword() {
		ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder().clientId("clientId")
				.secretHash(CognitoUtils.calculateSecretHash("clientId", "clientSecret", USERNAME)).username(USERNAME)
				.build();

		cognitoTemplate.resetPassword(USERNAME);

		verify(cognitoIdentityProviderClient).forgotPassword(forgotPasswordRequest);
	}

	@Test
	void confirmResetPassword() {
		ConfirmForgotPasswordRequest forgotPasswordRequest = ConfirmForgotPasswordRequest.builder().clientId("clientId")
				.username(USERNAME).password("newPassword").confirmationCode("confirmationCode")
				.secretHash(CognitoUtils.calculateSecretHash("clientId", "clientSecret", USERNAME)).build();

		cognitoTemplate.confirmResetPassword(USERNAME, "confirmationCode", "newPassword");

		verify(cognitoIdentityProviderClient).confirmForgotPassword(forgotPasswordRequest);
	}

	@Test
	void setPermanentPassword() {
		RespondToAuthChallengeRequest permanentPasswordRequest = RespondToAuthChallengeRequest.builder()
				.clientId("clientId").challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED).session("session")
				.challengeResponses(Map.of(CognitoParameters.USERNAME_PARAM_NAME, USERNAME,
						CognitoParameters.NEW_PASSWORD_PARAM_NAME, PASSWORD, CognitoParameters.SECRET_HASH_PARAM_NAME,
						CognitoUtils.calculateSecretHash("clientId", "clientSecret", USERNAME)))
				.build();

		cognitoTemplate.setPermanentPassword("session", USERNAME, PASSWORD);

		verify(cognitoIdentityProviderClient).respondToAuthChallenge(permanentPasswordRequest);
	}

	@Test
	void logout() {
		AdminUserGlobalSignOutRequest logoutRequest = AdminUserGlobalSignOutRequest.builder().userPoolId("userPoolId")
				.username(USERNAME).build();

		cognitoTemplate.logout(USERNAME);

		verify(cognitoIdentityProviderClient).adminUserGlobalSignOut(logoutRequest);
	}

	private List<AttributeType> createAttributes() {
		return List.of(AttributeType.builder().name("email").value("foo@bar.com").build());
	}

	private Map<String, String> resolveAuthParameters() {
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(CognitoParameters.USERNAME_PARAM_NAME, CognitoTemplateTest.USERNAME);
		parametersMap.put(CognitoParameters.PASSWORD_PARAM_NAME, CognitoTemplateTest.PASSWORD);
		parametersMap.put(CognitoParameters.SECRET_HASH_PARAM_NAME,
				CognitoUtils.calculateSecretHash("clientId", "clientSecret", CognitoTemplateTest.USERNAME));
		return parametersMap;
	}

}
