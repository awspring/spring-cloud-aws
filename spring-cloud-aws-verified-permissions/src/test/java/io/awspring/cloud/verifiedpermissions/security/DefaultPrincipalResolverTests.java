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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class DefaultPrincipalResolverTests {

	@Test
	void mapsNameAndRoles() {
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("alice", "pw", "ROLE_ADMIN",
				"SCOPE_read");

		PrincipalSource source = new DefaultPrincipalResolver().resolve(authentication);

		assertThat(source).isInstanceOfSatisfying(PrincipalSource.EntityPrincipal.class, principal -> {
			assertThat(principal.principal().ref()).isEqualTo(new EntityRef("User", "alice"));
			assertThat(principal.principal().parents()).containsExactly(new EntityRef("Role", "ADMIN"));
		});
	}

	@Test
	void returnsNullForAnonymousAndMissingAuthentication() {
		DefaultPrincipalResolver resolver = new DefaultPrincipalResolver();

		assertThat(resolver.resolve(null)).isNull();
		assertThat(resolver.resolve(new AnonymousAuthenticationToken("key", "anonymous",
				AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))).isNull();
	}

	@Test
	void sendsRawJwtInTokenMode() {
		Jwt jwt = Jwt.withTokenValue("raw.jwt.value").header("alg", "none").subject("alice").issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60)).build();

		PrincipalSource source = new DefaultPrincipalResolver(TokenMode.ACCESS_TOKEN, "User", "Role", "ROLE_")
				.resolve(new JwtAuthenticationToken(jwt, List.of()));

		assertThat(source).isEqualTo(PrincipalSource.accessToken("raw.jwt.value"));
	}

	@Test
	void tokenExpiryIsTrustedOnlyForTheTokenTheAuthenticationHolds() {
		Instant expiresAt = Instant.parse("2026-11-05T08:00:00Z");
		Jwt jwt = Jwt.withTokenValue("raw.jwt.value").header("alg", "none").subject("alice")
				.issuedAt(expiresAt.minusSeconds(60)).expiresAt(expiresAt).build();
		JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());

		assertThat(DefaultPrincipalResolver.tokenExpiry(authentication, "raw.jwt.value")).isEqualTo(expiresAt);
		assertThat(DefaultPrincipalResolver.tokenExpiry(authentication, "another.jwt.value")).isNull();
		assertThat(DefaultPrincipalResolver.tokenExpiry(new TestingAuthenticationToken("alice", "pw"), "raw.jwt.value"))
				.isNull();
		assertThat(DefaultPrincipalResolver.tokenExpiry(null, "raw.jwt.value")).isNull();
	}

	@Test
	void rejectsOpaqueTokensInTokenMode() {
		OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "opaque", Instant.now(),
				Instant.now().plusSeconds(60));
		BearerTokenAuthentication authentication = new BearerTokenAuthentication(
				new DefaultOAuth2AuthenticatedPrincipal("alice", Map.of("sub", "alice"), List.of()), token, List.of());

		assertThatThrownBy(() -> new DefaultPrincipalResolver(TokenMode.ACCESS_TOKEN, "User", "Role", "ROLE_")
				.resolve(authentication)).isInstanceOf(IllegalStateException.class).hasMessageContaining("Opaque");
	}

	@Test
	void rejectsNonTokenAuthenticationInTokenMode() {
		assertThatThrownBy(() -> new DefaultPrincipalResolver(TokenMode.IDENTITY_TOKEN, "User", "Role", "ROLE_")
				.resolve(new TestingAuthenticationToken("alice", "pw", "ROLE_USER")))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("IDENTITY_TOKEN");
	}
}
