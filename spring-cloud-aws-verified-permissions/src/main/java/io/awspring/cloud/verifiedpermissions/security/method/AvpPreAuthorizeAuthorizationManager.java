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
package io.awspring.cloud.verifiedpermissions.security.method;

import io.awspring.cloud.verifiedpermissions.security.VerifiedPermissionsAuthorizer;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

/**
 * {@link AuthorizationManager} for {@link AvpPreAuthorize} on blocking methods. Abstains (returns {@code null}) for
 * methods without the annotation.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class AvpPreAuthorizeAuthorizationManager implements AuthorizationManager<MethodInvocation> {

	private final Supplier<VerifiedPermissionsAuthorizer> authorizer;

	private final AvpExpressionEvaluator evaluator;

	/**
	 * @param authorizer looked up on first use, so the manager can be created early as infrastructure
	 * @param expressionHandler looked up on first use
	 */
	public AvpPreAuthorizeAuthorizationManager(Supplier<VerifiedPermissionsAuthorizer> authorizer,
			Supplier<MethodSecurityExpressionHandler> expressionHandler) {
		Assert.notNull(authorizer, "authorizer must not be null");
		this.authorizer = authorizer;
		this.evaluator = new AvpExpressionEvaluator(expressionHandler);
	}

	/**
	 * Wraps a manager for {@link AvpPreAuthorize} in the method interceptor that runs right after Spring Security's
	 * {@code @PreAuthorize}. Publish it from a {@code static} {@code @Bean} method with
	 * {@code @Role(BeanDefinition.ROLE_INFRASTRUCTURE)}; the auto-configuration does this for you.
	 */
	public static AuthorizationManagerBeforeMethodInterceptor interceptor(
			AuthorizationManager<MethodInvocation> manager) {
		return AvpMethodSecurityAdvisors.preAuthorize(manager);
	}

	@Override
	@Nullable
	public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
			MethodInvocation invocation) {
		AvpExpressionEvaluator.Attributes attributes = evaluator.attributes(invocation, AvpPreAuthorize.class);
		if (attributes == null) {
			return null;
		}
		AvpExpressionEvaluator.Target target = evaluator.evaluate(attributes, authentication, invocation, null, false);
		if (target.resource() == null) {
			throw new IllegalArgumentException(
					"@AvpPreAuthorize resource on " + invocation.getMethod() + " evaluated to null");
		}
		return authorizer.get().authorize(authentication.get(), target.action(), target.resource(), target.context(),
				invocation, null, target.tokenMode());
	}
}
