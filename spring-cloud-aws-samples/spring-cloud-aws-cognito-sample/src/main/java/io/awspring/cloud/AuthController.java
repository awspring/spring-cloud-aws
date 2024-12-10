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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;

/**
 * Demo controller for authentication operations.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final CognitoTemplate cognitoTemplate;

	private static final String USERNAME = "foo@bar.com";

	public AuthController(CognitoTemplate cognitoTemplate) {
		this.cognitoTemplate = cognitoTemplate;
	}

	@PostMapping("/signup")
	void signup(@RequestBody SignupRequest signupRequest) {
		cognitoTemplate.createUser(signupRequest.username(), signupRequest.password(), getAttributes(signupRequest));
	}

	@PostMapping("/login")
	LoginResponse login(@RequestBody LoginRequest loginRequest) {
		AdminInitiateAuthResponse response = cognitoTemplate.login(loginRequest.username(), loginRequest.password());
		LoginResponse loginResponse = new LoginResponse();
		if (ChallengeNameType.NEW_PASSWORD_REQUIRED.equals(response.challengeName())) {
			loginResponse.setSession(response.session());
			AuthResult authResult = new AuthResult();
			authResult.setStatus(Status.SET_PASSWORD);
			loginResponse.setAuthResult(authResult);
		}
		AuthenticationResultType authenticationResultType = response.authenticationResult();
		AuthResult authResult = new AuthResult();
		authResult.setAccessToken(authenticationResultType.accessToken());
		authResult.setIdToken(authenticationResultType.idToken());
		authResult.setRefreshToken(authenticationResultType.refreshToken());
		authResult.setStatus(Status.SUCCESS);
		loginResponse.setAuthResult(authResult);

		return loginResponse;
	}

	@PostMapping("/set-password")
	LoginResponse setPassword(@RequestBody SetPasswordRequest setPasswordRequest) {
		RespondToAuthChallengeResponse respondToAuthChallengeResponse = cognitoTemplate.setPermanentPassword(
				setPasswordRequest.session(), setPasswordRequest.username(), setPasswordRequest.newPassword());

		LoginResponse loginResponse = new LoginResponse();
		AuthResult authResult = new AuthResult();
		authResult.setAccessToken(respondToAuthChallengeResponse.authenticationResult().accessToken());
		authResult.setIdToken(respondToAuthChallengeResponse.authenticationResult().idToken());
		authResult.setRefreshToken(respondToAuthChallengeResponse.authenticationResult().refreshToken());
		loginResponse.setAuthResult(authResult);

		return loginResponse;
	}

	@PostMapping("/reset-password")
	void resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
		cognitoTemplate.resetPassword(resetPasswordRequest.username());
	}

	@PostMapping("/confirm-reset-password")
	void confirmResetPassword(@RequestBody ConfirmResetPasswordRequest confirmResetPasswordRequest) {
		cognitoTemplate.confirmResetPassword(confirmResetPasswordRequest.username(),
				confirmResetPasswordRequest.confirmationCode, confirmResetPasswordRequest.newPassword);
	}

	@PostMapping("/logout")
	void logout(@RequestBody LogoutRequest logoutRequest) {
		cognitoTemplate.logout(logoutRequest.username());
	}

	private List<AttributeType> getAttributes(SignupRequest signupRequest) {
		return List.of(AttributeType.builder().name("email").value(USERNAME).build(),
				AttributeType.builder().name("name").value(signupRequest.username()).build(),
				AttributeType.builder().name("custom:role").value("USER").build()
		// and all other attributes here
		);
	}

	record SignupRequest(String username, String password) {
	}

	record LoginRequest(String username, String password) {
	}

	public static class LoginResponse {
		String session;
		AuthResult authResult;

		public void setSession(String session) {
			this.session = session;
		}

		public void setAuthResult(AuthResult authResult) {
			this.authResult = authResult;
		}

		public String getSession() {
			return session;
		}

		public AuthResult getAuthResult() {
			return authResult;
		}
	}

	public static class AuthResult {
		String accessToken;
		String idToken;
		String refreshToken;
		Status status;

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public void setIdToken(String idToken) {
			this.idToken = idToken;
		}

		public void setRefreshToken(String refreshToken) {
			this.refreshToken = refreshToken;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public String getIdToken() {
			return idToken;
		}

		public String getRefreshToken() {
			return refreshToken;
		}

		public Status getStatus() {
			return status;
		}
	}

	public enum Status {
		SUCCESS, SET_PASSWORD
	}

	record SetPasswordRequest(String session, String username, String newPassword) {

	}

	record ResetPasswordRequest(String username) {

	}

	record ConfirmResetPasswordRequest(String username, String confirmationCode, String newPassword) {

	}

	record LogoutRequest(String username) {

	}

}
