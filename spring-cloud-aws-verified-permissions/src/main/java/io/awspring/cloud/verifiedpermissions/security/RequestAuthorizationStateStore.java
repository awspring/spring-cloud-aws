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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Locates request-local authorization state for servlet and non-web invocations.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
final class RequestAuthorizationStateStore {

	private static final String ATTRIBUTE = RequestAuthorizationStateStore.class.getName() + ".STATE";
	private static final ServletRequestAccess SERVLET = ServletRequestAccess.create();
	private final ThreadLocal<StateDepth> local = new ThreadLocal<>();

	public <T> T within(Supplier<T> operation) {
		if (SERVLET.current() != null) {
			current();
			return operation.get();
		}
		StateDepth holder = local.get();
		if (holder == null) {
			holder = new StateDepth(new RequestAuthorizationState());
			local.set(holder);
		}
		holder.depth++;
		try {
			return operation.get();
		}
		finally {
			if (--holder.depth == 0) {
				local.remove();
			}
		}
	}

	/**
	 * Returns the current {@code jakarta.servlet.http.HttpServletRequest} bound by Spring MVC, or {@code null}.
	 */
	@Nullable
	public Object currentRequest() {
		Object attributes = SERVLET.current();
		return attributes == null ? null : SERVLET.request(attributes);
	}

	public RequestAuthorizationState current() {
		Object request = SERVLET.current();
		if (request != null) {
			RequestAuthorizationState state = SERVLET.get(request);
			if (state == null) {
				state = new RequestAuthorizationState();
				SERVLET.set(request, state);
			}
			return state;
		}
		StateDepth holder = local.get();
		return holder == null ? new RequestAuthorizationState() : holder.state;
	}

	private static final class ServletRequestAccess {
		@Nullable
		private final Method current;
		@Nullable
		private final Method get;
		@Nullable
		private final Method set;
		@Nullable
		private final Class<?> servletAttributes;
		@Nullable
		private final Method request;

		private ServletRequestAccess(@Nullable Method current, @Nullable Method get, @Nullable Method set,
				@Nullable Class<?> servletAttributes, @Nullable Method request) {
			this.current = current;
			this.get = get;
			this.set = set;
			this.servletAttributes = servletAttributes;
			this.request = request;
		}

		static ServletRequestAccess create() {
			if (!ClassUtils.isPresent("org.springframework.web.context.request.RequestContextHolder",
					RequestAuthorizationStateStore.class.getClassLoader())) {
				return new ServletRequestAccess(null, null, null, null, null);
			}
			try {
				Class<?> holder = Class.forName("org.springframework.web.context.request.RequestContextHolder");
				Class<?> attributes = Class.forName("org.springframework.web.context.request.RequestAttributes");
				Class<?> servletAttributes = null;
				Method request = null;
				String servletAttributesName = "org.springframework.web.context.request.ServletRequestAttributes";
				if (ClassUtils.isPresent(servletAttributesName,
						RequestAuthorizationStateStore.class.getClassLoader())) {
					servletAttributes = Class.forName(servletAttributesName);
					request = servletAttributes.getMethod("getRequest");
				}
				return new ServletRequestAccess(holder.getMethod("getRequestAttributes"),
						attributes.getMethod("getAttribute", String.class, int.class),
						attributes.getMethod("setAttribute", String.class, Object.class, int.class), servletAttributes,
						request);
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Cannot initialize servlet request state access", e);
			}
		}

		@Nullable
		Object current() {
			return invoke(current, null);
		}

		@Nullable
		RequestAuthorizationState get(Object request) {
			return (RequestAuthorizationState) invoke(get, request, ATTRIBUTE, 0);
		}

		@Nullable
		Object request(Object attributes) {
			return servletAttributes != null && servletAttributes.isInstance(attributes) ? invoke(request, attributes)
					: null;
		}

		void set(Object request, RequestAuthorizationState state) {
			invoke(set, request, ATTRIBUTE, state, 0);
		}

		@Nullable
		private static Object invoke(@Nullable Method method, @Nullable Object target, Object... args) {
			if (method == null) {
				return null;
			}
			try {
				return method.invoke(target, args);
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
			catch (InvocationTargetException e) {
				throw e.getCause() instanceof RuntimeException runtime ? runtime
						: new IllegalStateException(e.getCause());
			}
		}
	}

	private static final class StateDepth {
		private final RequestAuthorizationState state;
		private int depth;

		private StateDepth(RequestAuthorizationState state) {
			this.state = state;
		}
	}
}
