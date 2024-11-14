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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;

/**
 * Higher level abstraction over {@link CognitoIdentityProviderClient} providing methods for the most common auth
 * operations
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

public class CognitoTemplate implements CognitoAuthOperations {

	private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
	private final String clientId;
	private final String userPoolId;
	private final String clientSecret;

	public CognitoTemplate(CognitoIdentityProviderClient cognitoIdentityProviderClient, String clientId,
			String userPoolId, String clientSecret) {
		Assert.notNull(cognitoIdentityProviderClient, "cognitoIdentityProviderClient is required");
		Assert.notNull(clientId, "clientId is required");
		Assert.notNull(userPoolId, "userPoolId is required");
		this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
		this.clientId = clientId;
		this.userPoolId = userPoolId;
		this.clientSecret = clientSecret;
	}

	@Override
	public AdminInitiateAuthResponse login(String username, String password) {
		AdminInitiateAuthRequest adminInitiateAuthRequest = AdminInitiateAuthRequest.builder().userPoolId(userPoolId)
				.clientId(clientId).authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
				.authParameters(resolveAuthParameters(username, password)).build();
		return cognitoIdentityProviderClient.adminInitiateAuth(adminInitiateAuthRequest);
	}

	@Override
	public AdminCreateUserResponse createUser(String username, List<AttributeType> attributeTypes) {
		AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder().userPoolId(userPoolId)
				.username(username).userAttributes(attributeTypes).build();
		return cognitoIdentityProviderClient.adminCreateUser(createUserRequest);
	}

	@Override
	public ForgotPasswordResponse resetPassword(String username) {
		ForgotPasswordRequest.Builder forgotPasswordRequestBuilder = ForgotPasswordRequest.builder().clientId(clientId)
				.username(username);
		if (this.clientSecret != null) {
			forgotPasswordRequestBuilder.secretHash(CognitoUtils.calculateSecretHash(clientId, clientSecret, username));
		}
		ForgotPasswordRequest forgotPasswordRequest = forgotPasswordRequestBuilder.build();

		return cognitoIdentityProviderClient.forgotPassword(forgotPasswordRequest);
	}

	@Override
	public ConfirmForgotPasswordResponse confirmResetPassword(String username, String confirmationCode,
			String newPassword) {
		ConfirmForgotPasswordRequest confirmForgotPasswordRequest = ConfirmForgotPasswordRequest.builder()
				.clientId(clientId).username(username).password(newPassword).confirmationCode(confirmationCode)
				.secretHash(CognitoUtils.calculateSecretHash(clientId, clientSecret, username)).build();
		return cognitoIdentityProviderClient.confirmForgotPassword(confirmForgotPasswordRequest);
	}

	@Override
	public RespondToAuthChallengeResponse setPermanentPassword(String session, String username, String password) {
		RespondToAuthChallengeRequest respondToAuthChallengeRequest = RespondToAuthChallengeRequest.builder()
				.clientId(clientId).challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED).session(session)
				.challengeResponses(Map.of(CognitoParameters.USERNAME_PARAM_NAME, username,
						CognitoParameters.NEW_PASSWORD_PARAM_NAME, password, CognitoParameters.SECRET_HASH_PARAM_NAME,
						CognitoUtils.calculateSecretHash(clientId, clientSecret, username)))
				.build();
		return cognitoIdentityProviderClient.respondToAuthChallenge(respondToAuthChallengeRequest);
	}

	private Map<String, String> resolveAuthParameters(String username, String password) {
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(CognitoParameters.USERNAME_PARAM_NAME, username);
		parametersMap.put(CognitoParameters.PASSWORD_PARAM_NAME, password);
		if (this.clientSecret != null) {
			parametersMap.put(CognitoParameters.SECRET_HASH_PARAM_NAME,
					CognitoUtils.calculateSecretHash(clientId, clientSecret, username));
		}
		return parametersMap;
	}
}
