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

import io.awspring.cloud.verifiedpermissions.EntityRef;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Blocking authorization data available to context providers.
 *
 * @param request the current {@code jakarta.servlet.http.HttpServletRequest} when the call runs on a Spring MVC request
 * thread, otherwise {@code null}
 * @author Matej Nedic
 * @since 4.3.0
 */
public record ServletAuthorizationContext(@Nullable Authentication authentication, String action, EntityRef resource,
		@Nullable MethodInvocation methodInvocation, @Nullable Object returnObject, @Nullable Object request) {
}
