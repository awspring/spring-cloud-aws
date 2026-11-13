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

import io.awspring.cloud.verifiedpermissions.security.TokenModeOverride;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Removes the returned elements the current user may not perform the action on. Elements are checked with
 * {@code BatchIsAuthorized}, 30 per call, so a list of N elements costs about N / 30 calls. Order is preserved and a
 * new collection is returned.
 * <p>
 * Supported return types: {@link java.util.Collection}, arrays and {@link java.util.stream.Stream}.
 *
 * <pre class="code">
 * &#64;AvpPostFilter(action = "ViewPhoto")
 * List&lt;Photo&gt; album(String albumId);
 * </pre>
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AvpPostFilter {

	/**
	 * The action id checked for every element.
	 */
	String action();

	/**
	 * Per-item SpEL expression for Cedar context. The current element is {@code filterObject}.
	 */
	String context() default "";

	/**
	 * Principal mode for this call, or {@link TokenModeOverride#DEFAULT}.
	 */
	TokenModeOverride tokenMode() default TokenModeOverride.DEFAULT;
}
