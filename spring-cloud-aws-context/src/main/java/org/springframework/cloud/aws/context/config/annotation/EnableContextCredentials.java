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

import org.springframework.context.annotation.Import;

/**
 * Enables a credentials provider configuration for the application context, that
 * credentials provider is used for all automatically created amazon web services client
 * (either through annotations or xml).
 *
 * @author Agim Emruli
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(ContextCredentialsConfigurationRegistrar.class)
@Target(ElementType.TYPE)
public @interface EnableContextCredentials {

	/**
	 * Configures the access key that will be used by the credentials provider.
	 * @return accessKey that should be used for all web service requests
	 */
	String accessKey() default "";

	/**
	 * Configures the secret key that will be used by the credentials provider.
	 * @return secretKey that should be used for all web service requests
	 */
	String secretKey() default "";

	/**
	 * Enables a instance profile specific credentials provider.
	 * @return true if the instance profile credentials provider should be configured
	 */
	boolean instanceProfile() default false;

	String profileName() default "";

	String profilePath() default "";

}
