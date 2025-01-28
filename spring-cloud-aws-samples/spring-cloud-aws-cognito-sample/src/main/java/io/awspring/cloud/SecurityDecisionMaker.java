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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Demo permission evaluator.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

@Service
public class SecurityDecisionMaker {

	public boolean hasPermission(Authentication authentication, Permission permission) {
		return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).map(Role::valueOf)
				.anyMatch(role -> role.hasPermission(permission));
	}
}
