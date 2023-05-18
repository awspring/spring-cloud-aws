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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.util.Assert;

/**
 * AWS Cognito implementation of {@link GrantedAuthoritiesMapper} that maps cognito user
 * groups to roles.
 *
 * @author Dominik Kovács
 * @since 3.0
 */
public class CognitoOidcUserAuthoritiesMapper implements GrantedAuthoritiesMapper, InitializingBean {

	private static final String COGNITO_GROUPS_KEY = "cognito:groups";

	private static final String OIDC_USER = "OIDC_USER";

	private boolean convertToUpperCase = false;

	private boolean convertToLowerCase = false;

	private String rolePrefix = "ROLE_";

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		return authorities.stream().map(OidcUserAuthority.class::cast)
				.filter(grantedAuthority -> OIDC_USER.equals(grantedAuthority.getAuthority())).findFirst()
				.map(this::extractGrantedAuthorities).orElse(Collections.emptySet());
	}

	private Set<GrantedAuthority> extractGrantedAuthorities(OidcUserAuthority oidcUserAuthority) {
		if (!oidcUserAuthority.getAttributes().containsKey(COGNITO_GROUPS_KEY)) {
			return Collections.emptySet();
		}
		return ((List<?>) oidcUserAuthority.getAttributes().get(COGNITO_GROUPS_KEY)).stream().map(String.class::cast)
				.map(this::groupToGrantedAuthority).collect(Collectors.toSet());
	}

	private GrantedAuthority groupToGrantedAuthority(String cognitoGroupName) {
		if (convertToUpperCase) {
			cognitoGroupName = cognitoGroupName.toUpperCase().replace('-', '_');
		}
		else if (convertToLowerCase) {
			cognitoGroupName = cognitoGroupName.toLowerCase().replace('_', '-');
		}

		String role = cognitoGroupName;
		if (rolePrefix.length() > 0 && !cognitoGroupName.startsWith(rolePrefix)) {
			role = rolePrefix + cognitoGroupName;
		}
		return new SimpleGrantedAuthority(role);
	}

	@Override
	public void afterPropertiesSet() {
		Assert.isTrue(!convertToUpperCase || !convertToLowerCase,
				"Either convertToUpperCase or convertToLowerCase can be set to true, but not both");
		Assert.notNull(rolePrefix, "Role prefix must not be null");
	}

	public void setConvertToUpperCase(boolean convertToUpperCase) {
		this.convertToUpperCase = convertToUpperCase;
	}

	public void setConvertToLowerCase(boolean convertToLowerCase) {
		this.convertToLowerCase = convertToLowerCase;
	}

	public void setRolePrefix(String rolePrefix) {
		this.rolePrefix = rolePrefix;
	}

}
