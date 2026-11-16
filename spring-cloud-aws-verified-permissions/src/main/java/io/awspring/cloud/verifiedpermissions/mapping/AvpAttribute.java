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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sends a field (or record component) of an {@link AvpEntity} as an entity attribute. Only annotated fields are sent.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AvpAttribute {

	/**
	 * The attribute name. Defaults to the field name.
	 */
	String value() default "";

	/**
	 * When set, the value is an entity id of this type and is sent as an entity reference, for example
	 * {@code @AvpAttribute(entityType = "User") String owner} becomes {@code owner: User::"alice"}. Collections become
	 * sets of references.
	 */
	String entityType() default "";
}
