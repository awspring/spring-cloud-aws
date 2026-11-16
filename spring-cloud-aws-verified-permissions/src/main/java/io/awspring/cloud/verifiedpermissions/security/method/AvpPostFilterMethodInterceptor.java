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

import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsRequestFactory;
import io.awspring.cloud.verifiedpermissions.security.VerifiedPermissionsAuthorizer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.Pointcut;
import org.springframework.core.CollectionFactory;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authorization.method.AuthorizationAdvisor;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.util.Assert;

/**
 * Applies {@link AvpPostFilter} to blocking methods. Runs right after Spring Security's own {@code @PostFilter}.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class AvpPostFilterMethodInterceptor implements AuthorizationAdvisor {

	private final Supplier<VerifiedPermissionsAuthorizer> authorizer;

	private final AvpExpressionEvaluator evaluator;

	private final Pointcut pointcut = AvpMethodSecurityAdvisors.pointcut(AvpPostFilter.class);

	private int order = AuthorizationInterceptorsOrder.POST_FILTER.getOrder() - 1;

	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
			.getContextHolderStrategy();

	public AvpPostFilterMethodInterceptor(Supplier<VerifiedPermissionsAuthorizer> authorizer,
			Supplier<MethodSecurityExpressionHandler> expressionHandler) {
		Assert.notNull(authorizer, "authorizer must not be null");
		this.authorizer = authorizer;
		this.evaluator = new AvpExpressionEvaluator(expressionHandler);
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object result = invocation.proceed();
		AvpExpressionEvaluator.Attributes attributes = evaluator.attributes(invocation, AvpPostFilter.class);
		if (attributes == null || result == null) {
			return result;
		}
		return filter(result, attributes, invocation);
	}

	private Object filter(Object result, AvpExpressionEvaluator.Attributes attributes, MethodInvocation invocation) {
		VerifiedPermissionsAuthorizer authorizer = this.authorizer.get();
		var authentication = securityContextHolderStrategy.getContext().getAuthentication();
		Function<Object, Map<String, ?>> context = element -> evaluator.evaluateFilterContext(attributes,
				() -> authentication, invocation, element);
		String action = attributes.action();
		if (result instanceof Collection<?> collection) {
			List<?> allowed = authorizer.filterAllowed(authentication, collection, action, context, invocation,
					attributes.tokenMode());
			Collection<Object> filtered = CollectionFactory.createApproximateCollection(collection, allowed.size());
			filtered.addAll(allowed);
			return filtered;
		}
		if (result.getClass().isArray()) {
			List<Object> elements = new ArrayList<>(Array.getLength(result));
			for (int i = 0; i < Array.getLength(result); i++) {
				elements.add(Array.get(result, i));
			}
			List<Object> allowed = authorizer.filterAllowed(authentication, elements, action, context, invocation,
					attributes.tokenMode());
			Object filtered = Array.newInstance(result.getClass().getComponentType(), allowed.size());
			for (int i = 0; i < allowed.size(); i++) {
				Array.set(filtered, i, allowed.get(i));
			}
			return filtered;
		}
		if (result instanceof Stream<?> stream) {
			return lazilyFiltered(stream, chunk -> authorizer.filterAllowed(authentication, chunk, action, context,
					invocation, attributes.tokenMode()));
		}
		throw new IllegalStateException("@AvpPostFilter supports Collection, array and Stream return values, got "
				+ result.getClass().getName() + ". Supported: " + Arrays.asList("Collection", "array", "Stream"));
	}

	/**
	 * Filters a stream lazily, pulling and authorizing at most one batch of {@code MAX_BATCH_SIZE} elements at a time,
	 * so large or unbounded streams are never buffered whole. Closing the result closes the source.
	 */
	private static Stream<Object> lazilyFiltered(Stream<?> source, Function<List<Object>, List<Object>> filter) {
		Iterator<?> iterator = source.iterator();
		Iterator<Object> filtered = new Iterator<>() {
			private Iterator<Object> current = Collections.emptyIterator();

			@Override
			public boolean hasNext() {
				while (!current.hasNext() && iterator.hasNext()) {
					List<Object> chunk = new ArrayList<>(VerifiedPermissionsRequestFactory.MAX_BATCH_SIZE);
					while (iterator.hasNext() && chunk.size() < VerifiedPermissionsRequestFactory.MAX_BATCH_SIZE) {
						chunk.add(iterator.next());
					}
					current = filter.apply(chunk).iterator();
				}
				return current.hasNext();
			}

			@Override
			public Object next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				return current.next();
			}
		};
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(filtered, Spliterator.ORDERED), false)
				.onClose(source::close);
	}

	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy must not be null");
		this.securityContextHolderStrategy = securityContextHolderStrategy;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public Pointcut getPointcut() {
		return pointcut;
	}

	@Override
	public Advice getAdvice() {
		return this;
	}

	@Override
	public boolean isPerInstance() {
		return true;
	}
}
