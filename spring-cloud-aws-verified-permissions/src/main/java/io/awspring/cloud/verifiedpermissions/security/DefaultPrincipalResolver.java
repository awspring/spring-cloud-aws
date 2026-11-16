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
package io.awspring.cloud.verifiedpermissions.security;

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default {@link PrincipalResolver}.
 * <ul>
 * <li>{@link TokenMode#ENTITY}: the principal is {@code <principalEntityType>::"<authentication name>"}. Each granted
 * authority that starts with the role prefix becomes a parent of type {@code groupEntityType} with the prefix removed,
 * so {@code ROLE_ADMIN} becomes {@code Role::"ADMIN"}.</li>
 * <li>{@link TokenMode#IDENTITY_TOKEN} / {@link TokenMode#ACCESS_TOKEN}: the raw JWT of a resource server
 * authentication (for example {@code JwtAuthenticationToken}) is sent. For {@code IDENTITY_TOKEN} the ID token of an
 * OIDC login is used too. Opaque tokens cannot be validated by Verified Permissions and are rejected.</li>
 * </ul>
 * Anonymous and unauthenticated callers resolve to {@code null}.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class DefaultPrincipalResolver implements PrincipalResolver {

	private static final boolean RESOURCE_SERVER_PRESENT = ClassUtils.isPresent(
			"org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken",
			DefaultPrincipalResolver.class.getClassLoader());

	private static final boolean OIDC_PRESENT = ClassUtils.isPresent(
			"org.springframework.security.oauth2.core.oidc.user.OidcUser",
			DefaultPrincipalResolver.class.getClassLoader());

	private final TokenMode tokenMode;

	private final String principalEntityType;

	private final String groupEntityType;

	private final String rolePrefix;

	public DefaultPrincipalResolver() {
		this(TokenMode.ENTITY, "User", "Role", "ROLE_");
	}

	public DefaultPrincipalResolver(TokenMode tokenMode, String principalEntityType, String groupEntityType,
			String rolePrefix) {
		Assert.notNull(tokenMode, "tokenMode must not be null");
		Assert.hasText(principalEntityType, "principalEntityType must not be empty");
		Assert.hasText(groupEntityType, "groupEntityType must not be empty");
		Assert.notNull(rolePrefix, "rolePrefix must not be null");
		this.tokenMode = tokenMode;
		this.principalEntityType = principalEntityType;
		this.groupEntityType = groupEntityType;
		this.rolePrefix = rolePrefix;
	}

	@Nullable
	public PrincipalSource resolve(@Nullable Authentication authentication) {
		return resolve(authentication, tokenMode);
	}

	@Override
	@Nullable
	public PrincipalSource resolve(@Nullable Authentication authentication, TokenMode tokenMode) {
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		return switch (tokenMode) {
		case ENTITY -> entityPrincipal(authentication);
		case IDENTITY_TOKEN -> PrincipalSource.identityToken(token(authentication, tokenMode));
		case ACCESS_TOKEN -> PrincipalSource.accessToken(token(authentication, tokenMode));
		};
	}

	private PrincipalSource entityPrincipal(Authentication authentication) {
		Set<EntityRef> groups = new LinkedHashSet<>();
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String name = authority.getAuthority();
			if (name != null && name.startsWith(rolePrefix) && name.length() > rolePrefix.length()) {
				groups.add(new EntityRef(groupEntityType, name.substring(rolePrefix.length())));
			}
		}
		Entity principal = new Entity(new EntityRef(principalEntityType, authentication.getName()), Map.of(), groups);
		return new PrincipalSource.EntityPrincipal(principal, List.of());
	}

	private String token(Authentication authentication, TokenMode tokenMode) {
		if (RESOURCE_SERVER_PRESENT) {
			String token = ResourceServerTokens.tokenValue(authentication);
			if (token != null) {
				return token;
			}
		}
		if (OIDC_PRESENT && tokenMode == TokenMode.IDENTITY_TOKEN) {
			String token = OidcTokens.idToken(authentication);
			if (token != null) {
				return token;
			}
		}
		throw new IllegalStateException("token-mode " + tokenMode + " needs a JWT bearer token or an OIDC login, but"
				+ " the authentication is a " + authentication.getClass().getName()
				+ ". Use token-mode entity or provide a PrincipalResolver bean.");
	}

	/**
	 * Returns the trusted expiry of {@code tokenValue} when it is the JWT of a resource server authentication or the ID
	 * token of an OIDC login held by {@code authentication}. Returns {@code null} when the token does not belong to the
	 * authentication or has no expiry, so callers never cache a token decision past an unknown lifetime. Works when the
	 * optional OAuth2 modules are absent.
	 */
	@Nullable
	public static Instant tokenExpiry(@Nullable Authentication authentication, String tokenValue) {
		if (authentication == null) {
			return null;
		}
		if (RESOURCE_SERVER_PRESENT) {
			Instant expiry = ResourceServerTokens.expiry(authentication, tokenValue);
			if (expiry != null) {
				return expiry;
			}
		}
		return OIDC_PRESENT ? OidcTokens.expiry(authentication, tokenValue) : null;
	}

	private static final class ResourceServerTokens {

		@Nullable
		static Instant expiry(Authentication authentication, String tokenValue) {
			if (authentication instanceof AbstractOAuth2TokenAuthenticationToken<?> token
					&& tokenValue.equals(token.getToken().getTokenValue())) {
				return token.getToken().getExpiresAt();
			}
			return null;
		}

		@Nullable
		static String tokenValue(Authentication authentication) {
			if (authentication instanceof BearerTokenAuthentication) {
				throw new IllegalStateException("Opaque bearer tokens cannot be validated by Verified Permissions."
						+ " Use JWTs, or token-mode entity with a PrincipalResolver that maps the introspected attributes.");
			}
			if (authentication instanceof AbstractOAuth2TokenAuthenticationToken<?> token) {
				return token.getToken().getTokenValue();
			}
			return null;
		}
	}

	private static final class OidcTokens {

		@Nullable
		static Instant expiry(Authentication authentication, String tokenValue) {
			if (authentication.getPrincipal() instanceof OidcUser user
					&& tokenValue.equals(user.getIdToken().getTokenValue())) {
				return user.getIdToken().getExpiresAt();
			}
			return null;
		}

		@Nullable
		static String idToken(Authentication authentication) {
			if (authentication.getPrincipal() instanceof OidcUser user) {
				return user.getIdToken().getTokenValue();
			}
			return null;
		}
	}
}
