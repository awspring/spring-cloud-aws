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
package io.awspring.cloud.verifiedpermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedOutputItem;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.Decision;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedWithTokenRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedWithTokenResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.ThrottlingException;

class VerifiedPermissionsTemplateTests {

	private final VerifiedPermissionsRequestFactory factory = new VerifiedPermissionsRequestFactory();

	private final PolicyStoreTarget target = new PolicyStoreTarget("ps", null);

	private final PrincipalSource alice = PrincipalSource.entity(new EntityRef("User", "alice"));

	private final AuthorizationRequest viewPhoto = AuthorizationRequest.of(alice, "ViewPhoto",
			new EntityRef("Photo", "42"));

	@Test
	void usesIsAuthorizedForEntityPrincipal() {
		VerifiedPermissionsClient client = mock(VerifiedPermissionsClient.class);
		when(client.isAuthorized(any(IsAuthorizedRequest.class)))
				.thenReturn(IsAuthorizedResponse.builder().decision(Decision.ALLOW).build());

		assertThat(new VerifiedPermissionsTemplate(client, factory).authorize(target, viewPhoto).allowed()).isTrue();
	}

	@Test
	void usesIsAuthorizedWithTokenForTokenPrincipal() {
		VerifiedPermissionsClient client = mock(VerifiedPermissionsClient.class);
		when(client.isAuthorizedWithToken(any(IsAuthorizedWithTokenRequest.class)))
				.thenReturn(IsAuthorizedWithTokenResponse.builder().decision(Decision.DENY).build());

		AuthorizationOutcome outcome = new VerifiedPermissionsTemplate(client, factory).authorize(target,
				AuthorizationRequest.of(PrincipalSource.identityToken("jwt"), "ViewPhoto",
						new EntityRef("Photo", "1")));

		assertThat(outcome.allowed()).isFalse();
		ArgumentCaptor<IsAuthorizedWithTokenRequest> captor = ArgumentCaptor
				.forClass(IsAuthorizedWithTokenRequest.class);
		verify(client).isAuthorizedWithToken(captor.capture());
		assertThat(captor.getValue().identityToken()).isEqualTo("jwt");
	}

	@Test
	void wrapsSdkErrors() {
		VerifiedPermissionsClient client = mock(VerifiedPermissionsClient.class);
		when(client.isAuthorized(any(IsAuthorizedRequest.class)))
				.thenThrow(ThrottlingException.builder().message("slow down").build());

		assertThatThrownBy(() -> new VerifiedPermissionsTemplate(client, factory).authorize(target, viewPhoto))
				.isInstanceOf(VerifiedPermissionsException.class).hasMessageContaining("IsAuthorized")
				.hasCauseInstanceOf(ThrottlingException.class);
	}

	@Test
	void sendsOneBatchCallPerThirtyItems() {
		VerifiedPermissionsClient client = mock(VerifiedPermissionsClient.class);
		when(client.batchIsAuthorized(any(BatchIsAuthorizedRequest.class)))
				.thenAnswer(invocation -> allow(invocation.getArgument(0)));
		BatchAuthorizationRequest request = new BatchAuthorizationRequest(alice,
				IntStream.range(0, 31).mapToObj(
						i -> BatchAuthorizationRequest.Item.of("ViewPhoto", new EntityRef("Photo", String.valueOf(i))))
						.toList(),
				List.of());

		List<AuthorizationOutcome> outcomes = new VerifiedPermissionsTemplate(client, factory).authorizeAll(target,
				request);

		assertThat(outcomes).hasSize(31).allMatch(AuthorizationOutcome::allowed);
		verify(client, times(2)).batchIsAuthorized(any(BatchIsAuthorizedRequest.class));
	}

	private static BatchIsAuthorizedResponse allow(BatchIsAuthorizedRequest request) {
		return BatchIsAuthorizedResponse.builder()
				.results(request.requests().stream().map(
						item -> BatchIsAuthorizedOutputItem.builder().request(item).decision(Decision.ALLOW).build())
						.toList())
				.build();
	}
}
