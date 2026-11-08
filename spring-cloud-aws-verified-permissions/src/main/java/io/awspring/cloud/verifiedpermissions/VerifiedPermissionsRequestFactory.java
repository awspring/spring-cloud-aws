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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.verifiedpermissions.model.ActionIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedInputItem;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedOutputItem;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedWithTokenInputItem;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedWithTokenOutputItem;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedWithTokenRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedWithTokenResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.ContextDefinition;
import software.amazon.awssdk.services.verifiedpermissions.model.Decision;
import software.amazon.awssdk.services.verifiedpermissions.model.DeterminingPolicyItem;
import software.amazon.awssdk.services.verifiedpermissions.model.EntitiesDefinition;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityIdentifier;
import software.amazon.awssdk.services.verifiedpermissions.model.EntityItem;
import software.amazon.awssdk.services.verifiedpermissions.model.EvaluationErrorItem;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedResponse;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedWithTokenRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.IsAuthorizedWithTokenResponse;

/**
 * Stateless translator for Verified Permissions authorization requests and responses.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class VerifiedPermissionsRequestFactory {

	public static final int MAX_BATCH_SIZE = 30;
	private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.withZone(ZoneOffset.UTC);

	public IsAuthorizedRequest isAuthorized(PolicyStoreTarget target, AuthorizationRequest request) {
		if (!(request.principal() instanceof PrincipalSource.EntityPrincipal principal)) {
			throw new IllegalArgumentException("IsAuthorized needs an entity principal, got " + request.principal());
		}
		List<Entity> all = new ArrayList<>();
		all.add(principal.principal());
		all.addAll(principal.relatedEntities());
		all.addAll(request.entities());
		return IsAuthorizedRequest.builder().policyStoreId(target.policyStoreId())
				.principal(identifier(target, principal.principal().ref())).action(action(target, request.action()))
				.resource(identifier(target, request.resource())).context(context(target, request.context()))
				.entities(entities(target, all)).build();
	}

	public IsAuthorizedWithTokenRequest isAuthorizedWithToken(PolicyStoreTarget target, AuthorizationRequest request) {
		checkTokenEntities(target, request.entities());
		IsAuthorizedWithTokenRequest.Builder builder = IsAuthorizedWithTokenRequest.builder()
				.policyStoreId(target.policyStoreId()).action(action(target, request.action()))
				.resource(identifier(target, request.resource())).context(context(target, request.context()))
				.entities(entities(target, request.entities()));
		applyToken(builder, request.principal());
		return builder.build();
	}

	public List<BatchIsAuthorizedRequest> batchIsAuthorized(PolicyStoreTarget target,
			io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest request) {
		if (!(request.principal() instanceof PrincipalSource.EntityPrincipal principal)) {
			throw new IllegalArgumentException(
					"BatchIsAuthorized needs an entity principal, got " + request.principal());
		}
		EntityIdentifier principalId = identifier(target, principal.principal().ref());
		List<BatchIsAuthorizedRequest> requests = new ArrayList<>();
		for (List<io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest.Item> chunk : chunks(
				request.items())) {
			List<Entity> all = new ArrayList<>();
			all.add(principal.principal());
			all.addAll(principal.relatedEntities());
			all.addAll(request.sharedEntities());
			chunk.forEach(item -> all.addAll(item.entities()));
			List<BatchIsAuthorizedInputItem> items = chunk.stream()
					.map(item -> BatchIsAuthorizedInputItem.builder().principal(principalId)
							.action(action(target, item.action())).resource(identifier(target, item.resource()))
							.context(context(target, item.context())).build())
					.toList();
			requests.add(BatchIsAuthorizedRequest.builder().policyStoreId(target.policyStoreId())
					.entities(entities(target, all)).requests(items).build());
		}
		return requests;
	}

	public List<BatchIsAuthorizedWithTokenRequest> batchIsAuthorizedWithToken(PolicyStoreTarget target,
			io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest request) {
		List<BatchIsAuthorizedWithTokenRequest> requests = new ArrayList<>();
		for (List<io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest.Item> chunk : chunks(
				request.items())) {
			List<Entity> all = new ArrayList<>(request.sharedEntities());
			chunk.forEach(item -> all.addAll(item.entities()));
			checkTokenEntities(target, all);
			List<BatchIsAuthorizedWithTokenInputItem> items = chunk.stream()
					.map(item -> BatchIsAuthorizedWithTokenInputItem.builder().action(action(target, item.action()))
							.resource(identifier(target, item.resource())).context(context(target, item.context()))
							.build())
					.toList();
			BatchIsAuthorizedWithTokenRequest.Builder builder = BatchIsAuthorizedWithTokenRequest.builder()
					.policyStoreId(target.policyStoreId()).entities(entities(target, all)).requests(items);
			applyToken(builder, request.principal());
			requests.add(builder.build());
		}
		return requests;
	}

	public AuthorizationOutcome outcome(IsAuthorizedResponse response) {
		return outcome(response.decision(), response.determiningPolicies(), response.errors());
	}

	public AuthorizationOutcome outcome(IsAuthorizedWithTokenResponse response) {
		return outcome(response.decision(), response.determiningPolicies(), response.errors());
	}

	public List<AuthorizationOutcome> outcomes(BatchIsAuthorizedRequest request, BatchIsAuthorizedResponse response) {
		return inRequestOrder(request.requests(), response.results(), BatchIsAuthorizedOutputItem::request,
				item -> outcome(item.decision(), item.determiningPolicies(), item.errors()));
	}

	public List<AuthorizationOutcome> outcomes(BatchIsAuthorizedWithTokenRequest request,
			BatchIsAuthorizedWithTokenResponse response) {
		return inRequestOrder(request.requests(), response.results(), BatchIsAuthorizedWithTokenOutputItem::request,
				item -> outcome(item.decision(), item.determiningPolicies(), item.errors()));
	}

	public String qualifyType(PolicyStoreTarget target, String type) {
		return target.qualifyType(type);
	}

	@Nullable
	AttributeValue attributeValue(@Nullable Object value) {
		return attributeValue(new PolicyStoreTarget("unused", null), value, "attribute");
	}

	private ActionIdentifier action(PolicyStoreTarget target, String action) {
		String type = target.namespace() == null ? "Action" : target.namespace() + "::Action";
		return ActionIdentifier.builder().actionType(type).actionId(action).build();
	}

	private EntityIdentifier identifier(PolicyStoreTarget target, EntityRef ref) {
		return EntityIdentifier.builder().entityType(qualifyType(target, ref.type())).entityId(ref.id()).build();
	}

	private void checkTokenEntities(PolicyStoreTarget target, Collection<Entity> entities) {
		Set<String> managed = new LinkedHashSet<>();
		target.tokenManagedEntityTypes().forEach(type -> managed.add(qualifyType(target, type)));
		for (Entity entity : entities) {
			if (managed.contains(qualifyType(target, entity.ref().type()))) {
				throw new IllegalStateException("Entity " + entity.ref()
						+ " cannot be sent with a token because Verified Permissions supplies it from the identity source");
			}
		}
	}

	private static void applyToken(IsAuthorizedWithTokenRequest.Builder builder, PrincipalSource principal) {
		if (principal instanceof PrincipalSource.IdentityToken token) {
			builder.identityToken(token.token());
		}
		else if (principal instanceof PrincipalSource.AccessToken token) {
			builder.accessToken(token.token());
		}
		else {
			throw new IllegalArgumentException("IsAuthorizedWithToken needs a token principal");
		}
	}

	private static void applyToken(BatchIsAuthorizedWithTokenRequest.Builder builder, PrincipalSource principal) {
		if (principal instanceof PrincipalSource.IdentityToken token) {
			builder.identityToken(token.token());
		}
		else if (principal instanceof PrincipalSource.AccessToken token) {
			builder.accessToken(token.token());
		}
		else {
			throw new IllegalArgumentException("BatchIsAuthorizedWithToken needs a token principal");
		}
	}

	private static List<List<io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest.Item>> chunks(
			List<io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest.Item> items) {
		List<List<io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest.Item>> chunks = new ArrayList<>();
		for (int i = 0; i < items.size(); i += MAX_BATCH_SIZE) {
			chunks.add(items.subList(i, Math.min(i + MAX_BATCH_SIZE, items.size())));
		}
		return chunks;
	}

	private static <I, O> List<AuthorizationOutcome> inRequestOrder(List<I> requested, List<O> results,
			Function<O, I> echoedRequest, Function<O, AuthorizationOutcome> outcome) {
		if (results.size() != requested.size()) {
			throw new IllegalStateException("Verified Permissions returned " + results.size() + " results for "
					+ requested.size() + " requests");
		}
		List<AuthorizationOutcome> outcomes = new ArrayList<>(requested.size());
		for (int i = 0; i < requested.size(); i++) {
			if (!requested.get(i).equals(echoedRequest.apply(results.get(i)))) {
				throw new IllegalStateException("Verified Permissions returned a mismatched result at index " + i);
			}
			outcomes.add(outcome.apply(results.get(i)));
		}
		return outcomes;
	}

	private static AuthorizationOutcome outcome(@Nullable Decision decision, List<DeterminingPolicyItem> policies,
			List<EvaluationErrorItem> errors) {
		return new AuthorizationOutcome(decision == Decision.ALLOW,
				policies.stream().map(DeterminingPolicyItem::policyId).toList(),
				errors.stream().map(EvaluationErrorItem::errorDescription).toList());
	}

	private EntitiesDefinition entities(PolicyStoreTarget target, Collection<Entity> source) {
		Map<EntityRef, Entity> unique = new LinkedHashMap<>();
		source.forEach(
				entity -> unique.merge(target.qualify(entity.ref()), entity, VerifiedPermissionsRequestFactory::merge));
		List<EntityItem> items = unique.values().stream()
				.map(entity -> EntityItem.builder().identifier(identifier(target, entity.ref()))
						.attributes(attributes(target, entity.attributes(), "entity " + entity.ref()))
						.parents(entity.parents().stream().map(parent -> identifier(target, parent)).toList()).build())
				.toList();
		return EntitiesDefinition.builder().entityList(items).build();
	}

	private static Entity merge(Entity first, Entity second) {
		Map<String, @Nullable Object> attributes = new LinkedHashMap<>(second.attributes());
		attributes.putAll(first.attributes());
		Set<EntityRef> parents = new LinkedHashSet<>(first.parents());
		parents.addAll(second.parents());
		return new Entity(first.ref(), attributes, parents);
	}

	private ContextDefinition context(PolicyStoreTarget target, Map<String, @Nullable Object> source) {
		return ContextDefinition.builder().contextMap(attributes(target, source, "context")).build();
	}

	private Map<String, AttributeValue> attributes(PolicyStoreTarget target, Map<?, ?> values, String path) {
		Map<String, AttributeValue> result = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : values.entrySet()) {
			if (!(entry.getKey() instanceof String key) || key.isBlank()) {
				throw new IllegalArgumentException(path + " keys must be nonblank Strings, got " + entry.getKey());
			}
			AttributeValue converted = attributeValue(target, entry.getValue(), path + "." + key);
			if (converted != null && result.putIfAbsent(key, converted) != null) {
				throw new IllegalArgumentException(path + " contains duplicate key " + key);
			}
		}
		return result;
	}

	@Nullable
	private AttributeValue attributeValue(PolicyStoreTarget target, @Nullable Object value, String path) {
		if (value == null) {
			return null;
		}
		if (value instanceof Optional<?> optional) {
			return attributeValue(target, optional.orElse(null), path);
		}
		if (value instanceof AttributeValue attributeValue) {
			return attributeValue;
		}
		if (value instanceof CharSequence || value instanceof Character) {
			return AttributeValue.fromString(value.toString());
		}
		if (value instanceof Enum<?> enumValue) {
			return AttributeValue.fromString(enumValue.name());
		}
		if (value instanceof Boolean bool) {
			return AttributeValue.fromBooleanValue(bool);
		}
		if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
			return AttributeValue.fromLongValue(((Number) value).longValue());
		}
		if (value instanceof BigInteger integer) {
			return AttributeValue.fromLongValue(integer.longValueExact());
		}
		if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
			if (value instanceof Double doubleValue && !Double.isFinite(doubleValue)
					|| value instanceof Float floatValue && !Float.isFinite(floatValue)) {
				throw new IllegalArgumentException(path + " must be a finite decimal");
			}
			BigDecimal decimal = value instanceof BigDecimal number ? number
					: BigDecimal.valueOf(((Number) value).doubleValue());
			String text = decimal.toPlainString();
			return AttributeValue.fromDecimal(text.contains(".") ? text : text + ".0");
		}
		if (value instanceof EntityRef ref) {
			return AttributeValue.fromEntityIdentifier(identifier(target, ref));
		}
		if (value instanceof Entity entity) {
			return AttributeValue.fromEntityIdentifier(identifier(target, entity.ref()));
		}
		if (value instanceof ExtensionValue extension) {
			return switch (extension.kind()) {
			case IPADDR -> AttributeValue.fromIpaddr(extension.value());
			case DECIMAL -> AttributeValue.fromDecimal(extension.value());
			case DATETIME -> AttributeValue.fromDatetime(extension.value());
			case DURATION -> AttributeValue.fromDuration(extension.value());
			};
		}
		if (value instanceof InetAddress address) {
			return AttributeValue.fromIpaddr(address.getHostAddress());
		}
		if (value instanceof Instant instant) {
			return AttributeValue.fromDatetime(DATETIME.format(instant.truncatedTo(ChronoUnit.MILLIS)));
		}
		if (value instanceof OffsetDateTime dateTime) {
			return attributeValue(target, dateTime.toInstant(), path);
		}
		if (value instanceof ZonedDateTime dateTime) {
			return attributeValue(target, dateTime.toInstant(), path);
		}
		if (value instanceof LocalDateTime dateTime) {
			return attributeValue(target, dateTime.toInstant(ZoneOffset.UTC), path);
		}
		if (value instanceof LocalDate date) {
			return AttributeValue.fromDatetime(date.toString());
		}
		if (value instanceof Duration duration) {
			return AttributeValue.fromDuration(duration.toMillis() + "ms");
		}
		if (value instanceof Map<?, ?> map) {
			return AttributeValue.fromRecord(attributes(target, map, path));
		}
		if (value instanceof Collection<?> collection) {
			return AttributeValue.fromSet(collection.stream().map(element -> attributeValue(target, element, path))
					.filter(java.util.Objects::nonNull).toList());
		}
		if (value.getClass().isArray()) {
			List<AttributeValue> elements = new ArrayList<>();
			for (int i = 0; i < Array.getLength(value); i++) {
				AttributeValue converted = attributeValue(target, Array.get(value, i), path + "[" + i + "]");
				if (converted != null) {
					elements.add(converted);
				}
			}
			return AttributeValue.fromSet(elements);
		}
		throw new IllegalArgumentException("Cannot convert " + value.getClass().getName() + " at " + path
				+ " to a Cedar value. Use a supported type, an EntityRef or an ExtensionValue.");
	}
}
