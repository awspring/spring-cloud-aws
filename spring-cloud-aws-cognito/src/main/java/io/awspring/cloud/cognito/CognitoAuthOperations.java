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

import java.util.List;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;

/**
 * An Interface for the most common Cognito auth operations
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

public interface CognitoAuthOperations {

	/**
	 * Logs in a user using username and password
	 * @param username - the username
	 * @param password - the password
	 * @return {@link AdminInitiateAuthResponse} a result of login operation from the AWS Cognito
	 */
	AdminInitiateAuthResponse login(String username, String password);

	/**
	 * Creates a new user with provided attributes
	 * @param username - the username
	 * @param attributeTypes - the list of user attributes defined by user pool
	 * @return {@link AdminCreateUserResponse} a result of user creation operation from the AWS Cognito
	 */
	AdminCreateUserResponse createUser(String username, List<AttributeType> attributeTypes);

	/**
	 * Resets password for a user
	 * @param username - the username
	 * @return {@link ForgotPasswordResponse} a result of password reset operation from the AWS Cognito
	 */
	ForgotPasswordResponse resetPassword(String username);

	/**
	 * Confirms password reset
	 * @param username - the username
	 * @param confirmationCode - the confirmation code for password reset operation
	 * @param newPassword - the new password
	 * @return {@link ConfirmForgotPasswordResponse} a result of password reset confirmation operation from the AWS
	 * Cognito
	 */
	ConfirmForgotPasswordResponse confirmResetPassword(String username, String confirmationCode, String newPassword);

	/**
	 * Sets a permanent password for a new user
	 * @param session - the session id returned by the login operation
	 * @param username - the username of the user
	 * @param password - the permanent password for user's account
	 * @return {@link RespondToAuthChallengeResponse} a result of setting permanent password operation from the AWS
	 * Cognito
	 */
	RespondToAuthChallengeResponse setPermanentPassword(String session, String username, String password);

}
