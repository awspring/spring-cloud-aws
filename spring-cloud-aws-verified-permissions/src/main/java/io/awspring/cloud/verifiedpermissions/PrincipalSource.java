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

import java.util.List;
import org.springframework.util.Assert;

/**
 * Where the principal of an authorization request comes from.
 * <ul>
 * <li>{@link EntityPrincipal}: the application describes the principal. Uses {@code IsAuthorized} and
 * {@code BatchIsAuthorized}.</li>
 * <li>{@link IdentityToken} / {@link AccessToken}: Verified Permissions validates the JWT against the policy store's
 * identity source and builds the principal from it. Uses {@code IsAuthorizedWithToken} and
 * {@code BatchIsAuthorizedWithToken}. ID token claims become principal attributes; access token claims are placed in
 * {@code context.token}.</li>
 * </ul>
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public sealed

interface PrincipalSource
permits PrincipalSource.EntityPrincipal,PrincipalSource.IdentityToken,PrincipalSource.AccessToken
{

	static EntityPrincipal entity(Entity principal) {
		return new EntityPrincipal(principal, List.of());
	}

	static EntityPrincipal entity(EntityRef principal) {
		return new EntityPrincipal(Entity.of(principal), List.of());
	}

	static IdentityToken identityToken(String token) {
		return new IdentityToken(token);
	}

	static AccessToken accessToken(String token) {
		return new AccessToken(token);
	}

	/**
	 * A principal described by the application.
	 *
	 * @param principal the principal entity
	 * @param relatedEntities other entities the policies need about the principal, for example its groups
	 */
	record EntityPrincipal(Entity principal, List<Entity> relatedEntities) implements PrincipalSource {
		public EntityPrincipal {
			Assert.notNull(principal, "principal must not be null");
			relatedEntities = List.copyOf(relatedEntities);
		}
	}

	/**
	 * A principal taken from an OIDC ID token.
	 *
	 * @param token the raw JWT
	 */
	record IdentityToken(String token) implements PrincipalSource {
		public IdentityToken {
			Assert.hasText(token, "token must not be empty");
		}

	@Override
		public String toString() {
			return "IdentityToken[***]";
		}}

	/**
	 * A principal taken from an OAuth2 access token.
	 *
	 * @param token the raw JWT
	 */
	record AccessToken(String token) implements PrincipalSource {
		public AccessToken {
			Assert.hasText(token, "token must not be empty");
		}

	@Override
		public String toString() {
			return "AccessToken[***]";
		}
}}
