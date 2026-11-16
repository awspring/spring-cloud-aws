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

import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Turns the current Spring Security {@link Authentication} into the principal of a Verified Permissions request.
 * Declare a bean to replace {@link DefaultPrincipalResolver}.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@FunctionalInterface
public interface PrincipalResolver {

	/**
	 * @param authentication the current authentication, may be {@code null}
	 * @return the principal, or {@code null} when the caller is not authenticated. Access is then denied without
	 * calling Verified Permissions.
	 */
	@Nullable
	PrincipalSource resolve(@Nullable Authentication authentication, TokenMode tokenMode);
}
