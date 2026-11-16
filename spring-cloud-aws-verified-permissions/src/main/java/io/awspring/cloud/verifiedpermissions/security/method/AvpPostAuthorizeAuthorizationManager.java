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
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.AuthorizationManagerAfterMethodInterceptor;
import org.springframework.security.authorization.method.MethodInvocationResult;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

/**
 * {@link AuthorizationManager} for {@link AvpPostAuthorize} on blocking methods. A {@code null} resource is denied when
 * {@code denyNullResource} is {@code true}, the default, and abstains otherwise.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class AvpPostAuthorizeAuthorizationManager implements AuthorizationManager<MethodInvocationResult> {

	private final Supplier<VerifiedPermissionsAuthorizer> authorizer;

	private final AvpExpressionEvaluator evaluator;

	private final boolean denyNullResource;

	public AvpPostAuthorizeAuthorizationManager(Supplier<VerifiedPermissionsAuthorizer> authorizer,
			Supplier<MethodSecurityExpressionHandler> expressionHandler) {
		this(authorizer, expressionHandler, true);
	}

	public AvpPostAuthorizeAuthorizationManager(Supplier<VerifiedPermissionsAuthorizer> authorizer,
			Supplier<MethodSecurityExpressionHandler> expressionHandler, boolean denyNullResource) {
		Assert.notNull(authorizer, "authorizer must not be null");
		this.authorizer = authorizer;
		this.evaluator = new AvpExpressionEvaluator(expressionHandler);
		this.denyNullResource = denyNullResource;
	}

	/**
	 * Wraps a manager for {@link AvpPostAuthorize} in the method interceptor that runs right after Spring Security's
	 * {@code @PostAuthorize}. Publish it from a {@code static} {@code @Bean} method with
	 * {@code @Role(BeanDefinition.ROLE_INFRASTRUCTURE)}; the auto-configuration does this for you.
	 */
	public static AuthorizationManagerAfterMethodInterceptor interceptor(
			AuthorizationManager<MethodInvocationResult> manager) {
		return AvpMethodSecurityAdvisors.postAuthorize(manager);
	}

	@Override
	@Nullable
	public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
			MethodInvocationResult result) {
		AvpExpressionEvaluator.Attributes attributes = evaluator.attributes(result.getMethodInvocation(),
				AvpPostAuthorize.class);
		if (attributes == null) {
			return null;
		}
		AvpExpressionEvaluator.Target target = evaluator.evaluate(attributes, authentication,
				result.getMethodInvocation(), result.getResult(), true);
		if (target.resource() == null) {
			return denyNullResource ? io.awspring.cloud.verifiedpermissions.security.AvpAuthorizationResult.denied(
					target.action(), io.awspring.cloud.verifiedpermissions.EntityRef.of("Unknown", "null"),
					"post-authorization resource is null") : null;
		}
		return authorizer.get().authorize(authentication.get(), target.action(), target.resource(), target.context(),
				result.getMethodInvocation(), result.getResult(), target.tokenMode());
	}
}
