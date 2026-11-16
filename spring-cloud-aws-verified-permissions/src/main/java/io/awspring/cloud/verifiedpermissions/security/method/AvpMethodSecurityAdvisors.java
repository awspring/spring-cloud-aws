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

import java.lang.annotation.Annotation;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.authorization.method.AuthorizationManagerAfterMethodInterceptor;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.MethodInvocationResult;

/**
 * Builds the Spring Security interceptors for {@link AvpPreAuthorize} and {@link AvpPostAuthorize} on blocking methods,
 * and the pointcut shared by the blocking interceptors. Each interceptor runs right after its Spring Security
 * counterpart ({@code @PreAuthorize}, {@code @PostAuthorize}), so both kinds can be combined on one method and all must
 * pass. Applications use {@link AvpPreAuthorizeAuthorizationManager#interceptor} and
 * {@link AvpPostAuthorizeAuthorizationManager#interceptor}.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
final class AvpMethodSecurityAdvisors {

	private AvpMethodSecurityAdvisors() {
	}

	static AuthorizationManagerBeforeMethodInterceptor preAuthorize(AuthorizationManager<MethodInvocation> manager) {
		AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
				pointcut(AvpPreAuthorize.class), manager);
		interceptor.setOrder(AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder() + 1);
		return interceptor;
	}

	static AuthorizationManagerAfterMethodInterceptor postAuthorize(
			AuthorizationManager<MethodInvocationResult> manager) {
		AuthorizationManagerAfterMethodInterceptor interceptor = new AuthorizationManagerAfterMethodInterceptor(
				pointcut(AvpPostAuthorize.class), manager);
		interceptor.setOrder(AuthorizationInterceptorsOrder.POST_AUTHORIZE.getOrder() - 1);
		return interceptor;
	}

	/**
	 * Matches methods annotated with the given annotation, and all methods of annotated classes.
	 */
	static Pointcut pointcut(Class<? extends Annotation> annotationType) {
		return new ComposablePointcut((Pointcut) new AnnotationMatchingPointcut(null, annotationType, true))
				.union(new AnnotationMatchingPointcut(annotationType, true));
	}
}
