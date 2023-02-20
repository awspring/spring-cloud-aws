/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/**
 * Tests for {@link CognitoOidcUserAuthoritiesMapper}.
 *
 * @author Dominik Kovács
 */
class CognitoOidcUserAuthoritiesMapperTest {

	private static final String COGNITO_GROUPS_KEY = "cognito:groups";

	private final GrantedAuthoritiesMapper authoritiesMapper = new CognitoOidcUserAuthoritiesMapper();

	@Test
	void mapAuthoritiesSuccessfully() {
		Map<String, Object> claims = Map.of(COGNITO_GROUPS_KEY, List.of("test-group"));
		OidcIdToken idToken = new OidcIdToken("test", Instant.now(), Instant.MAX, claims);
		OidcUserAuthority oidcUserAuthority = new OidcUserAuthority(idToken);

		// Create a collection of authorities to be mapped
		Set<? extends GrantedAuthority> authorities = Set.of(oidcUserAuthority);

		// Call the authorities mapper
		Collection<? extends GrantedAuthority> mappedAuthorities = authoritiesMapper.mapAuthorities(authorities);

		// Assert that the expected authorities are returned
		assertEquals("ROLE_test-group", mappedAuthorities.iterator().next().getAuthority());
	}

}
