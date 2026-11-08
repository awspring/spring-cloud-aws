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

import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.security.TokenModeOverride;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

/**
 * Finds the Verified Permissions annotation of a method and evaluates its SpEL attributes with Spring Security's
 * {@link MethodSecurityExpressionHandler}. Parsed expressions are cached per method and target class.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
final class AvpExpressionEvaluator {

	private final Supplier<MethodSecurityExpressionHandler> expressionHandler;

	private final Map<CacheKey, Optional<Attributes>> cache = new ConcurrentHashMap<>();

	public AvpExpressionEvaluator(Supplier<MethodSecurityExpressionHandler> expressionHandler) {
		this.expressionHandler = expressionHandler;
	}

	@Nullable
	public Attributes attributes(MethodInvocation invocation, Class<? extends Annotation> annotationType) {
		Method method = invocation.getMethod();
		Object target = invocation.getThis();
		Class<?> targetClass = target != null ? AopUtils.getTargetClass(target) : method.getDeclaringClass();
		return cache.computeIfAbsent(new CacheKey(method, targetClass, annotationType),
				key -> Optional.ofNullable(parse(method, targetClass, annotationType))).orElse(null);
	}

	public Target evaluate(Attributes attributes, Supplier<? extends @Nullable Authentication> authentication,
			MethodInvocation invocation, @Nullable Object returnObject, boolean afterInvocation) {
		MethodSecurityExpressionHandler handler = expressionHandler.get();
		EvaluationContext context = handler.createEvaluationContext(authentication, invocation);
		if (afterInvocation) {
			handler.setReturnObject(returnObject, context);
		}
		Object resource = null;
		if (attributes.resourceId() != null && attributes.resourceType() != null) {
			Object id = attributes.resourceId().getValue(context);
			if (id == null) {
				throw new IllegalArgumentException("resourceId expression '"
						+ attributes.resourceId().getExpressionString() + "' evaluated to null");
			}
			resource = EntityRef.of(attributes.resourceType(), id);
		}
		else if (attributes.resource() != null) {
			resource = attributes.resource().getValue(context);
		}
		Map<String, @Nullable Object> cedarContext = new LinkedHashMap<>();
		if (attributes.context() != null) {
			Object value = attributes.context().getValue(context);
			if (value instanceof Map<?, ?> map) {
				map.forEach((key, element) -> {
					if (!(key instanceof String text) || text.isBlank()) {
						throw new IllegalArgumentException(
								"context expression keys must be nonblank Strings, got " + key);
					}
					if (cedarContext.putIfAbsent(text, element) != null) {
						throw new IllegalArgumentException("context expression contains duplicate key " + text);
					}
				});
			}
			else if (value != null) {
				throw new IllegalArgumentException("context expression '" + attributes.context().getExpressionString()
						+ "' must evaluate to a Map, got " + value.getClass().getName());
			}
		}
		return new Target(attributes.action(), resource, cedarContext, attributes.tokenMode());
	}

	public Map<String, @Nullable Object> evaluateFilterContext(Attributes attributes,
			Supplier<? extends @Nullable Authentication> authentication, MethodInvocation invocation,
			Object filterObject) {
		if (attributes.context() == null) {
			return Map.of();
		}
		EvaluationContext context = expressionHandler.get().createEvaluationContext(authentication, invocation);
		if (context instanceof StandardEvaluationContext standard) {
			standard.setVariable("filterObject", filterObject);
		}
		Object value = attributes.context().getValue(context);
		if (!(value instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException(
					"context expression '" + attributes.context().getExpressionString() + "' must evaluate to a Map");
		}
		Map<String, @Nullable Object> result = new LinkedHashMap<>();
		map.forEach((key, element) -> {
			if (!(key instanceof String text) || text.isBlank()) {
				throw new IllegalArgumentException("context expression keys must be nonblank Strings, got " + key);
			}
			result.put(text, element);
		});
		return result;
	}

	@Nullable
	private Attributes parse(Method method, Class<?> targetClass, Class<? extends Annotation> annotationType) {
		MergedAnnotation<? extends Annotation> annotation = find(method, targetClass, annotationType);
		if (annotation == null) {
			return null;
		}
		ExpressionParser parser = expressionHandler.get().getExpressionParser();
		String action = annotation.getString("action");
		if (!StringUtils.hasText(action)) {
			action = StringUtils.capitalize(method.getName());
		}
		if (annotationType == AvpPostFilter.class) {
			String contextExpression = annotation.getString("context");
			return new Attributes(action, null, null, null,
					StringUtils.hasText(contextExpression) ? parser.parseExpression(contextExpression) : null,
					annotation.getEnum("tokenMode", TokenModeOverride.class));
		}
		String resource = annotation.getString("resource");
		String resourceType = annotation.getString("resourceType");
		String resourceId = annotation.getString("resourceId");
		String contextExpression = annotation.getString("context");
		boolean byId = StringUtils.hasText(resourceId);
		if (byId != StringUtils.hasText(resourceType)) {
			throw new IllegalStateException("@" + annotationType.getSimpleName() + " on " + method
					+ " must set resourceType and resourceId together");
		}
		boolean explicitResource = StringUtils.hasText(resource)
				&& !(annotationType == AvpPostAuthorize.class && "returnObject".equals(resource));
		if (byId && explicitResource) {
			throw new IllegalStateException("@" + annotationType.getSimpleName() + " on " + method
					+ " sets both resource and resourceType/resourceId; use one");
		}
		if (!byId && !StringUtils.hasText(resource)) {
			throw new IllegalStateException("@" + annotationType.getSimpleName() + " on " + method
					+ " needs a resource expression or resourceType and resourceId");
		}
		return new Attributes(action, byId ? null : parser.parseExpression(resource), byId ? resourceType : null,
				byId ? parser.parseExpression(resourceId) : null,
				StringUtils.hasText(contextExpression) ? parser.parseExpression(contextExpression) : null,
				annotation.getEnum("tokenMode", TokenModeOverride.class));
	}

	@Nullable
	private static MergedAnnotation<? extends Annotation> find(Method method, Class<?> targetClass,
			Class<? extends Annotation> annotationType) {
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		for (MergedAnnotations annotations : new MergedAnnotations[] {
				MergedAnnotations.from(specificMethod, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY),
				MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY),
				MergedAnnotations.from(targetClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY),
				MergedAnnotations.from(method.getDeclaringClass(), MergedAnnotations.SearchStrategy.TYPE_HIERARCHY) }) {
			MergedAnnotation<? extends Annotation> annotation = annotations.get(annotationType);
			if (annotation.isPresent()) {
				return annotation;
			}
		}
		return null;
	}

	public record Attributes(String action, @Nullable Expression resource, @Nullable String resourceType,
			@Nullable Expression resourceId, @Nullable Expression context, TokenModeOverride tokenMode) {
	}

	public record Target(String action, @Nullable Object resource, Map<String, @Nullable Object> context,
			TokenModeOverride tokenMode) {
	}

	private record CacheKey(Method method, Class<?> targetClass, Class<? extends Annotation> annotationType) {
	}
}
