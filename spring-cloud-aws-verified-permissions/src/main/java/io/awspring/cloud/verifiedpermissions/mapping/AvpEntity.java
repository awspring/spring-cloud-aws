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
package io.awspring.cloud.verifiedpermissions.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Verified Permissions entity, so instances can be passed as resources and are sent with their
 * attributes. One field or record component must be annotated with {@link AvpEntityId}.
 *
 * <pre class="code">
 * &#64;AvpEntity("Photo")
 * public record Photo(&#64;AvpEntityId String id, &#64;AvpAttribute(entityType = "User") String owner,
 * 		&#64;AvpAttribute boolean isPrivate, &#64;AvpParent("Album") String albumId) {
 * }
 * </pre>
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AvpEntity {

	/**
	 * The entity type, without namespace unless it differs from the configured one.
	 */
	String value();
}
