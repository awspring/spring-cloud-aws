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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Demo authentication converter.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

@Component
public class AuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final String ROLE_AUTHORITIES = "custom:role";
	private final JwtAuthenticationConverter jwtAuthenticationConverter;

	public AuthenticationConverter() {
		this.jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> getAuthorities(jwt.getClaims()));
		jwtAuthenticationConverter.setPrincipalClaimName("email");
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt source) {
		return jwtAuthenticationConverter.convert(source);
	}

	private Collection<GrantedAuthority> getAuthorities(Map<String, Object> map) {
		if (!map.containsKey(ROLE_AUTHORITIES)) {
			return Collections.emptyList();
		}
		String role = (String) map.get(ROLE_AUTHORITIES);
		return List.of(new SimpleGrantedAuthority(role));
	}
}
