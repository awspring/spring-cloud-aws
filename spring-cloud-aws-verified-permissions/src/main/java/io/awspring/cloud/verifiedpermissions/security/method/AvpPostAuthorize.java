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
 * Asks Verified Permissions after the method returned, using the returned object as the resource by default.
 *
 * <pre class="code">
 * &#64;AvpPostAuthorize(action = "ViewPhoto")
 * Photo find(String id);
 * </pre>
 *
 * A {@code null} resource is denied unless {@code method-security.deny-null-post-resource} is {@code false}, in which
 * case it is not checked.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AvpPostAuthorize {

	/**
	 * The action id. Defaults to the method name with its first letter in upper case.
	 */
	String action() default "";

	/**
	 * SpEL expression for the resource; {@code returnObject} is the returned value.
	 */
	String resource() default "returnObject";

	/**
	 * Resource type, used with {@link #resourceId()}.
	 */
	String resourceType() default "";

	/**
	 * SpEL expression for the resource id, used with {@link #resourceType()}, for example {@code returnObject.id}.
	 */
	String resourceId() default "";

	/**
	 * SpEL expression for the Cedar context; must evaluate to a {@link java.util.Map}.
	 */
	String context() default "";

	/**
	 * Principal mode for this call, or {@link TokenModeOverride#DEFAULT}.
	 */
	TokenModeOverride tokenMode() default TokenModeOverride.DEFAULT;
}
