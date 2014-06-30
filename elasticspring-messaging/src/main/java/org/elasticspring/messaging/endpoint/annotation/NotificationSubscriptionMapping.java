/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.endpoint.annotation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Agim Emruli
 */
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(headers = "x-amz-sns-message-type=SubscriptionConfirmation")
@ResponseStatus(HttpStatus.NO_CONTENT)
public @interface NotificationSubscriptionMapping {

	/**
	 * @see {@link org.springframework.web.bind.annotation.RequestMapping#value()}
	 */
	String[] value() default {};

	/**
	 * @see {@link org.springframework.web.bind.annotation.RequestMapping#method()}
	 */
	RequestMethod[] method() default {};

	/**
	 * @see {@link org.springframework.web.bind.annotation.RequestMapping#params()}
	 */
	String[] params() default {};

	/**
	 * @see {@link org.springframework.web.bind.annotation.RequestMapping#headers()}
	 */
	String[] headers() default {};

	/**
	 * @see {@link org.springframework.web.bind.annotation.RequestMapping#consumes()}
	 */
	String[] consumes() default {};

	/**
	 * @see {@link org.springframework.web.bind.annotation.RequestMapping#produces()}
	 */
	String[] produces() default {};
}