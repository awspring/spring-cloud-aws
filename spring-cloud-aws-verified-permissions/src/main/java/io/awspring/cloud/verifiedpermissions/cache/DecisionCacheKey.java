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
package io.awspring.cloud.verifiedpermissions.cache;

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.ExtensionValue;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import io.awspring.cloud.verifiedpermissions.security.TokenMode;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;

/**
 * Complete, digest-based identity of an authorization decision. Raw tokens, context and entity attributes are never
 * retained; they are reduced to SHA-256 digests of a canonical form.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class DecisionCacheKey {

	private final String cachePartition;
	private final String policyStoreId;
	@Nullable
	private final String namespace;
	private final TokenMode tokenMode;
	private final String principalDigest;
	private final String action;
	private final EntityRef resource;
	private final String contextDigest;
	private final String entityGraphDigest;

	DecisionCacheKey(String cachePartition, String policyStoreId, @Nullable String namespace, TokenMode tokenMode,
			String principalDigest, String action, EntityRef resource, String contextDigest, String entityGraphDigest) {
		this.cachePartition = cachePartition;
		this.policyStoreId = policyStoreId;
		this.namespace = namespace;
		this.tokenMode = tokenMode;
		this.principalDigest = principalDigest;
		this.action = action;
		this.resource = resource;
		this.contextDigest = contextDigest;
		this.entityGraphDigest = entityGraphDigest;
	}

	/**
	 * Creates the key of one authorization decision.
	 */
	public static DecisionCacheKey of(PolicyStoreTarget target, TokenMode tokenMode, PrincipalSource principal,
			String action, EntityRef resource, Map<String, ?> context, Collection<Entity> entities) {
		return new DecisionCacheKey(target.cachePartition(), target.policyStoreId(), target.namespace(), tokenMode,
				digest(principal), action, resource, digest(context), digest(entities));
	}

	/**
	 * The application-controlled partition, used by {@link DecisionCache#evictPartition(String)}.
	 */
	public String cachePartition() {
		return cachePartition;
	}

	public String policyStoreId() {
		return policyStoreId;
	}

	public String action() {
		return action;
	}

	public EntityRef resource() {
		return resource;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return this == other || other instanceof DecisionCacheKey key && cachePartition.equals(key.cachePartition)
				&& policyStoreId.equals(key.policyStoreId) && Objects.equals(namespace, key.namespace)
				&& tokenMode == key.tokenMode && principalDigest.equals(key.principalDigest)
				&& action.equals(key.action) && resource.equals(key.resource) && contextDigest.equals(key.contextDigest)
				&& entityGraphDigest.equals(key.entityGraphDigest);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cachePartition, policyStoreId, namespace, tokenMode, principalDigest, action, resource,
				contextDigest, entityGraphDigest);
	}

	@Override
	public String toString() {
		return "DecisionCacheKey[partition=" + cachePartition + ", policyStore=" + policyStoreId + ", action=" + action
				+ ", resource=" + resource + "]";
	}

	private static String digest(Object value) {
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Prefix-free, type-tagged encoding: every value starts with a one-letter tag, every string is length-prefixed, and
	 * every composite carries its element count. Distinct values sent to Verified Permissions therefore never encode to
	 * the same text. Values that cannot be sent are rejected rather than encoded through {@code toString()}, which may
	 * omit or redact fields.
	 */
	static String canonical(@Nullable Object value) {
		StringBuilder out = new StringBuilder();
		encode(value, out);
		return out.toString();
	}

	private static void encode(@Nullable Object value, StringBuilder out) {
		if (value == null) {
			out.append('n');
		}
		else if (value instanceof Optional<?> optional) {
			encode(optional.orElse(null), out);
		}
		else if (value instanceof PrincipalSource.IdentityToken token) {
			text('I', token.token(), out);
		}
		else if (value instanceof PrincipalSource.AccessToken token) {
			text('A', token.token(), out);
		}
		else if (value instanceof PrincipalSource.EntityPrincipal principal) {
			out.append('P');
			encode(principal.principal(), out);
			encode(principal.relatedEntities(), out);
		}
		else if (value instanceof Entity entity) {
			out.append('E');
			encode(entity.ref(), out);
			encode(entity.attributes(), out);
			encode(entity.parents(), out);
		}
		else if (value instanceof EntityRef ref) {
			out.append('r');
			text('s', ref.type(), out);
			text('s', ref.id(), out);
		}
		else if (value instanceof AttributeValue attribute) {
			encodeAttribute(attribute, out);
		}
		else if (value instanceof CharSequence || value instanceof Character) {
			text('s', value.toString(), out);
		}
		else if (value instanceof Enum<?> enumValue) {
			text('s', enumValue.name(), out);
		}
		else if (value instanceof Boolean bool) {
			out.append(bool ? "b1" : "b0");
		}
		else if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte
				|| value instanceof BigInteger) {
			text('l', value.toString(), out);
		}
		else if (value instanceof BigDecimal decimal) {
			text('d', decimal.stripTrailingZeros().toPlainString(), out);
		}
		else if (value instanceof Double || value instanceof Float) {
			text('d', BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros().toPlainString(), out);
		}
		else if (value instanceof ExtensionValue extension) {
			out.append('x');
			text('s', extension.kind().name(), out);
			text('s', extension.value(), out);
		}
		else if (value instanceof InetAddress address) {
			text('a', address.getHostAddress(), out);
		}
		else if (value instanceof TemporalAccessor || value instanceof Duration) {
			out.append('t');
			text('s', value.getClass().getName(), out);
			text('s', value.toString(), out);
		}
		else if (value instanceof Map<?, ?> map) {
			List<String> entries = new ArrayList<>(map.size());
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				entries.add(canonical(entry.getKey()) + canonical(entry.getValue()));
			}
			composite('m', entries, out);
		}
		else if (value instanceof Collection<?> collection) {
			composite('c', collection.stream().map(DecisionCacheKey::canonical).toList(), out);
		}
		else if (value.getClass().isArray()) {
			List<String> elements = new ArrayList<>();
			for (int i = 0; i < Array.getLength(value); i++) {
				elements.add(canonical(Array.get(value, i)));
			}
			composite('c', elements, out);
		}
		else {
			throw new IllegalArgumentException("Cannot build a decision cache key for " + value.getClass().getName()
					+ "; use a supported Cedar value type");
		}
	}

	private static void encodeAttribute(AttributeValue value, StringBuilder out) {
		AttributeValue.Type type = value.type();
		if (type == null) {
			throw new IllegalArgumentException("Cannot build a decision cache key for an empty AttributeValue");
		}
		switch (type) {
		case BOOLEAN -> out.append(Boolean.TRUE.equals(value.booleanValue()) ? "b1" : "b0");
		case ENTITY_IDENTIFIER -> {
			out.append('r');
			text('s', value.entityIdentifier().entityType(), out);
			text('s', value.entityIdentifier().entityId(), out);
		}
		case LONG -> text('l', String.valueOf(value.longValue()), out);
		case STRING -> text('s', value.string(), out);
		case DECIMAL -> text('d', new BigDecimal(value.decimal()).stripTrailingZeros().toPlainString(), out);
		case IPADDR -> text('a', value.ipaddr(), out);
		case DATETIME -> {
			out.append('x');
			text('s', "DATETIME", out);
			text('s', value.datetime(), out);
		}
		case DURATION -> {
			out.append('x');
			text('s', "DURATION", out);
			text('s', value.duration(), out);
		}
		case SET -> composite('c', value.set().stream().map(DecisionCacheKey::canonical).toList(), out);
		case RECORD -> {
			List<String> entries = new ArrayList<>(value.record().size());
			value.record().forEach((key, element) -> entries.add(canonical(key) + canonical(element)));
			composite('m', entries, out);
		}
		default ->
			throw new IllegalArgumentException("Cannot build a decision cache key for AttributeValue type " + type);
		}
	}

	private static void text(char tag, String value, StringBuilder out) {
		out.append(tag).append(value.length()).append(':').append(value);
	}

	/**
	 * Maps and sets are unordered in Cedar, so encoded elements are sorted before they are joined.
	 */
	private static void composite(char tag, List<String> elements, StringBuilder out) {
		List<String> sorted = new ArrayList<>(elements);
		Collections.sort(sorted);
		out.append(tag).append(sorted.size()).append(':');
		sorted.forEach(out::append);
	}
}
