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

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedOutputItem;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.Decision;
import software.amazon.awssdk.services.verifiedpermissions.model.DeterminingPolicyItem;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityItem;
import software.amazon.awssdk.services.verifiedpermissions.model.EvaluationErrorItem;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedWithTokenRequest;

class VerifiedPermissionsRequestFactoryTests {

	private final VerifiedPermissionsRequestFactory factory = new VerifiedPermissionsRequestFactory();

	private final PolicyStoreTarget target = new PolicyStoreTarget("policy-store-alias/photos", "PhotoFlash");

	private final PrincipalSource alice = new PrincipalSource.EntityPrincipal(
			new Entity(new EntityRef("User", "alice"), Map.of(), Set.of(new EntityRef("Role", "ADMIN"))), List.of());

	@Test
	void buildsIsAuthorizedRequestWithNamespaceAndEntities() {
		Entity photo = new Entity(new EntityRef("Photo", "42"),
				Map.of("owner", new EntityRef("User", "alice"), "private", true), Set.of(new EntityRef("Album", "a1")));
		AuthorizationRequest request = new AuthorizationRequest(alice, "ViewPhoto", photo.ref(), Map.of("mfa", true),
				List.of(photo));

		IsAuthorizedRequest sdkRequest = factory.isAuthorized(target, request);

		assertThat(sdkRequest.policyStoreId()).isEqualTo("policy-store-alias/photos");
		assertThat(sdkRequest.principal()).isEqualTo(id("PhotoFlash::User", "alice"));
		assertThat(sdkRequest.action().actionType()).isEqualTo("PhotoFlash::Action");
		assertThat(sdkRequest.action().actionId()).isEqualTo("ViewPhoto");
		assertThat(sdkRequest.resource()).isEqualTo(id("PhotoFlash::Photo", "42"));
		assertThat(sdkRequest.context().contextMap()).containsEntry("mfa", AttributeValue.fromBooleanValue(true));
		List<EntityItem> entities = sdkRequest.entities().entityList();
		assertThat(entities).extracting(EntityItem::identifier).containsExactly(id("PhotoFlash::User", "alice"),
				id("PhotoFlash::Photo", "42"));
		assertThat(entities.get(0).parents()).containsExactly(id("PhotoFlash::Role", "ADMIN"));
		assertThat(entities.get(1).attributes()).containsEntry("owner",
				AttributeValue.fromEntityIdentifier(id("PhotoFlash::User", "alice")));
		assertThat(entities.get(1).parents()).containsExactly(id("PhotoFlash::Album", "a1"));
	}

	@Test
	void keepsTypesThatAlreadyHaveANamespace() {
		assertThat(factory.qualifyType(target, "Other::Photo")).isEqualTo("Other::Photo");
		assertThat(factory.qualifyType(new PolicyStoreTarget("ps", null), "Photo")).isEqualTo("Photo");
	}

	@Test
	void convertsJavaValuesToCedarValues() {
		Map<String, Object> nested = new LinkedHashMap<>();
		nested.put("level", 3);
		nested.put("missing", null);

		assertThat(factory.attributeValue("text")).isEqualTo(AttributeValue.fromString("text"));
		assertThat(factory.attributeValue(7)).isEqualTo(AttributeValue.fromLongValue(7L));
		assertThat(factory.attributeValue(new BigDecimal("1.5"))).isEqualTo(AttributeValue.fromDecimal("1.5"));
		assertThat(factory.attributeValue(new BigDecimal("2"))).isEqualTo(AttributeValue.fromDecimal("2.0"));
		assertThat(factory.attributeValue(InetAddress.getLoopbackAddress()))
				.isEqualTo(AttributeValue.fromIpaddr("127.0.0.1"));
		assertThat(factory.attributeValue(Instant.parse("2026-11-04T05:57:00.123456Z")))
				.isEqualTo(AttributeValue.fromDatetime("2026-11-04T05:57:00.123Z"));
		assertThat(factory.attributeValue(Duration.ofMinutes(90))).isEqualTo(AttributeValue.fromDuration("5400000ms"));
		assertThat(factory.attributeValue(ExtensionValue.ipaddr("10.0.0.0/8")))
				.isEqualTo(AttributeValue.fromIpaddr("10.0.0.0/8"));
		assertThat(factory.attributeValue(List.of("a", "b"))).isEqualTo(
				AttributeValue.fromSet(List.of(AttributeValue.fromString("a"), AttributeValue.fromString("b"))));
		assertThat(factory.attributeValue(nested))
				.isEqualTo(AttributeValue.fromRecord(Map.of("level", AttributeValue.fromLongValue(3L))));
		assertThat(factory.attributeValue(null)).isNull();
		assertThatThrownBy(() -> factory.attributeValue(new Object())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("java.lang.Object");
	}

	@Test
	void buildsTokenRequest() {
		AuthorizationRequest request = AuthorizationRequest.of(PrincipalSource.accessToken("jwt"), "ViewPhoto",
				new EntityRef("Photo", "42"));

		IsAuthorizedWithTokenRequest sdkRequest = factory.isAuthorizedWithToken(target, request);

		assertThat(sdkRequest.accessToken()).isEqualTo("jwt");
		assertThat(sdkRequest.identityToken()).isNull();
		assertThat(sdkRequest.resource()).isEqualTo(id("PhotoFlash::Photo", "42"));
	}

	@Test
	void rejectsPrincipalEntitiesInTokenRequests() {
		PolicyStoreTarget tokenTarget = new PolicyStoreTarget("policy-store-alias/photos", "PhotoFlash",
				Set.of("User", "Role"), "photos");
		AuthorizationRequest request = new AuthorizationRequest(PrincipalSource.identityToken("jwt"), "ViewPhoto",
				new EntityRef("Photo", "42"), Map.of(), List.of(Entity.of(new EntityRef("User", "bob"))));

		assertThatThrownBy(() -> factory.isAuthorizedWithToken(tokenTarget, request))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("User::\"bob\"");
	}

	@Test
	void acceptsMoreThanOneHundredEntities() {
		List<Entity> albums = IntStream.range(0, 150).mapToObj(i -> Entity.of(new EntityRef("Album", "a" + i)))
				.toList();
		AuthorizationRequest request = new AuthorizationRequest(alice, "ViewPhoto", new EntityRef("Photo", "42"),
				Map.of(), albums);

		assertThat(factory.isAuthorized(target, request).entities().entityList()).hasSize(151);
	}

	@Test
	void mergesEntitiesThatDifferOnlyByNamespacePrefix() {
		Entity plain = new Entity(new EntityRef("Photo", "42"), Map.of("private", true), Set.of());
		Entity qualified = new Entity(new EntityRef("PhotoFlash::Photo", "42"), Map.of("owner", "alice"),
				Set.of(new EntityRef("Album", "a1")));
		AuthorizationRequest request = new AuthorizationRequest(alice, "ViewPhoto", plain.ref(), Map.of(),
				List.of(plain, qualified));

		List<EntityItem> entities = factory.isAuthorized(target, request).entities().entityList();

		assertThat(entities).extracting(EntityItem::identifier).containsExactly(id("PhotoFlash::User", "alice"),
				id("PhotoFlash::Photo", "42"));
		assertThat(entities.get(1).attributes()).containsKeys("private", "owner");
		assertThat(entities.get(1).parents()).containsExactly(id("PhotoFlash::Album", "a1"));
	}

	@Test
	void splitsBatchesIntoChunksOfThirty() {
		List<BatchAuthorizationRequest.Item> items = IntStream.range(0, 65)
				.mapToObj(i -> BatchAuthorizationRequest.Item.of("ViewPhoto", new EntityRef("Photo", "p" + i)))
				.toList();

		List<BatchIsAuthorizedRequest> requests = factory.batchIsAuthorized(target,
				new BatchAuthorizationRequest(alice, items, List.of()));

		assertThat(requests).extracting(request -> request.requests().size()).containsExactly(30, 30, 5);
		assertThat(requests.get(2).requests().get(4).resource()).isEqualTo(id("PhotoFlash::Photo", "p64"));
		assertThat(requests.get(0).requests().get(0).principal()).isEqualTo(id("PhotoFlash::User", "alice"));
	}

	@Test
	void rejectsBatchResultsThatDoNotMatchRequestOrder() {
		BatchIsAuthorizedRequest request = factory.batchIsAuthorized(target, new BatchAuthorizationRequest(alice,
				List.of(BatchAuthorizationRequest.Item.of("ViewPhoto", new EntityRef("Photo", "1")),
						BatchAuthorizationRequest.Item.of("ViewPhoto", new EntityRef("Photo", "2"))),
				List.of())).get(0);
		List<BatchIsAuthorizedOutputItem> results = new ArrayList<>();
		results.add(BatchIsAuthorizedOutputItem.builder().request(request.requests().get(1)).decision(Decision.DENY)
				.build());
		results.add(BatchIsAuthorizedOutputItem.builder().request(request.requests().get(0)).decision(Decision.ALLOW)
				.determiningPolicies(DeterminingPolicyItem.builder().policyId("p-1").build()).build());

		assertThatThrownBy(
				() -> factory.outcomes(request, BatchIsAuthorizedResponse.builder().results(results).build()))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("index 0");
	}

	@Test
	void mapsDecisionPoliciesAndErrors() {
		AuthorizationOutcome outcome = factory.outcome(IsAuthorizedResponse.builder().decision(Decision.DENY)
				.errors(EvaluationErrorItem.builder().errorDescription("type error").build()).build());

		assertThat(outcome.allowed()).isFalse();
		assertThat(outcome.errors()).containsExactly("type error");
	}

	@Test
	void includesOnlyCurrentChunkEntitySlices() {
		List<BatchAuthorizationRequest.Item> items = IntStream.range(0, 65).mapToObj(i -> {
			Entity photo = Entity.of(new EntityRef("Photo", "p" + i));
			return new BatchAuthorizationRequest.Item("ViewPhoto", photo.ref(), Map.of(), List.of(photo));
		}).toList();

		List<BatchIsAuthorizedRequest> requests = factory.batchIsAuthorized(target,
				new BatchAuthorizationRequest(alice, items, List.of()));

		assertThat(requests).extracting(request -> request.entities().entityList().size()).containsExactly(31, 31, 6);
	}

	@Test
	void rejectsUnsafeRecordKeysAndDecimals() {
		assertThatThrownBy(() -> factory.attributeValue(Map.of(1, "number")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nonblank Strings");
		assertThatThrownBy(() -> factory.attributeValue(Double.NaN)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("finite");
	}

	private static EntityIdentifier id(String type, String id) {
		return EntityIdentifier.builder().entityType(type).entityId(id).build();
	}
}
