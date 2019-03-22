/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.context.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.annotation.Import;

/**
 * Enables a {@link org.springframework.core.env.PropertySource} that resolve instance
 * meta-data through the amazon meta data service that is available on EC instances.
 *
 * <b>Note:</b>This annotation does not have any effect outside the EC2 environment.
 *
 * @author Agim Emruli
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ContextInstanceDataConfiguration.class)
public @interface EnableContextInstanceData {

	/**
	 * Allows to configure the value separator for the user data configured attributes.
	 * These is by default the ':' character following the Spring general place holder
	 * support {@link PlaceholderConfigurerSupport}
	 * @return the custom configured value separator
	 */
	String valueSeparator() default PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

	/**
	 * Allows to configure the attribute separator to separate multiple attributes in one
	 * global user data string.
	 * @return the custom configured attribute separator or ';' as the default
	 */
	String attributeSeparator() default ";";

}
