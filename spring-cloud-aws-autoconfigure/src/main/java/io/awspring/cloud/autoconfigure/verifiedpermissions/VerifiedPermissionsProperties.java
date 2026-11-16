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
package io.awspring.cloud.autoconfigure.verifiedpermissions;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.verifiedpermissions.security.TokenMode;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Properties for Amazon Verified Permissions.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@ConfigurationProperties(prefix = VerifiedPermissionsProperties.PREFIX)
public class VerifiedPermissionsProperties extends AwsClientProperties implements Validator {

	public static final String PREFIX = "spring.cloud.aws.verified-permissions";

	/** Policy store id or alias. */
	@Nullable
	private String policyStoreId;
	/** Cedar namespace added to unqualified types. */
	@Nullable
	private String namespace;
	/** Default principal mode. */
	private TokenMode tokenMode = TokenMode.ENTITY;
	/** Entity type used for entity principals. */
	private String principalEntityType = "User";
	/** Parent type used for mapped authorities. */
	private String groupEntityType = "Role";
	/** Prefix removed from authorities mapped to groups. */
	private String rolePrefix = "ROLE_";
	private final MethodSecurity methodSecurity = new MethodSecurity();
	private final EntityLoading entityLoading = new EntityLoading();
	private final RequestDeduplication requestDeduplication = new RequestDeduplication();
	private final Cache cache = new Cache();

	@Nullable
	public String getPolicyStoreId() {
		return policyStoreId;
	}

	public void setPolicyStoreId(@Nullable String policyStoreId) {
		this.policyStoreId = policyStoreId;
	}

	@Nullable
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(@Nullable String namespace) {
		this.namespace = namespace;
	}

	public TokenMode getTokenMode() {
		return tokenMode;
	}

	public void setTokenMode(TokenMode tokenMode) {
		this.tokenMode = tokenMode;
	}

	public String getPrincipalEntityType() {
		return principalEntityType;
	}

	public void setPrincipalEntityType(String principalEntityType) {
		this.principalEntityType = principalEntityType;
	}

	public String getGroupEntityType() {
		return groupEntityType;
	}

	public void setGroupEntityType(String groupEntityType) {
		this.groupEntityType = groupEntityType;
	}

	public String getRolePrefix() {
		return rolePrefix;
	}

	public void setRolePrefix(String rolePrefix) {
		this.rolePrefix = rolePrefix;
	}

	public MethodSecurity getMethodSecurity() {
		return methodSecurity;
	}

	public EntityLoading getEntityLoading() {
		return entityLoading;
	}

	public RequestDeduplication getRequestDeduplication() {
		return requestDeduplication;
	}

	public Cache getCache() {
		return cache;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return VerifiedPermissionsProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		VerifiedPermissionsProperties properties = (VerifiedPermissionsProperties) target;
		if (!StringUtils.hasText(properties.getPolicyStoreId())) {
			errors.rejectValue("policyStoreId", "required", "Policy store id must be provided");
		}
	}

	public static class MethodSecurity {
		/** Whether AVP method-security infrastructure is registered. */
		private boolean enabled = true;
		/** Whether a null post-authorization resource is denied instead of skipped. */
		private boolean denyNullPostResource = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isDenyNullPostResource() {
			return denyNullPostResource;
		}

		public void setDenyNullPostResource(boolean denyNullPostResource) {
			this.denyNullPostResource = denyNullPostResource;
		}
	}

	public static class EntityLoading {
		/** Maximum related-entity traversal depth. */
		private int maxDepth = 2;

		public int getMaxDepth() {
			return maxDepth;
		}

		public void setMaxDepth(int maxDepth) {
			this.maxDepth = maxDepth;
		}

	}

	public static class RequestDeduplication {
		/** Whether request-local entity and decision deduplication is enabled. */
		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class Cache {
		/** Whether cross-request decision caching is enabled. */
		private boolean enabled;
		/**
		 * Built-in cache strategy. Caffeine is the production default; concurrent is for tests and small applications.
		 */
		private CacheStrategy strategy = CacheStrategy.CAFFEINE;
		/** Maximum decision lifetime. */
		private Duration ttl = Duration.ofSeconds(10);
		/** Maximum number of cached decisions. */
		private long maxSize = 10000;
		/** Safety margin subtracted from trusted token expiry. */
		private Duration tokenExpirySkew = Duration.ofSeconds(5);

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public CacheStrategy getStrategy() {
			return strategy;
		}

		public void setStrategy(CacheStrategy strategy) {
			this.strategy = strategy;
		}

		public Duration getTtl() {
			return ttl;
		}

		public void setTtl(Duration ttl) {
			this.ttl = ttl;
		}

		public long getMaxSize() {
			return maxSize;
		}

		public void setMaxSize(long maxSize) {
			this.maxSize = maxSize;
		}

		public Duration getTokenExpirySkew() {
			return tokenExpirySkew;
		}

		public void setTokenExpirySkew(Duration tokenExpirySkew) {
			this.tokenExpirySkew = tokenExpirySkew;
		}
	}

	public enum CacheStrategy {
		CONCURRENT, CAFFEINE
	}
}
