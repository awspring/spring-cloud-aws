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

package org.springframework.cloud.aws.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Conditional(OnMissingAmazonClientCondition.class)
public @interface ConditionalOnMissingAmazonClient {

	/**
	 * <p>
	 * The Amazon clients that needs to be available in order to match the condition.
	 * </p>
	 *
	 * <b>IMPORTANT</b>: This condition does not verify the presence of a client, based on
	 * the type, but based on the default name as computed in
	 * {@link org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils#getBeanName}.
	 * @return Amazon client class
	 */
	Class<?> value();

}
