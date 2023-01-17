/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sns.annotation.endpoint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Spring Web MVC request mapping that supports Amazon SNS HTTP endpoint subscriptions using the Spring Controller
 * model. This annotation configures a method to receive notification unsubscriptions if the user does not want that a
 * controller receives any further notification. An annotated {@link NotificationUnsubscribeConfirmationMapping} will
 * receive a {@link io.awspring.cloud.sns.handlers.NotificationStatus} parameter and can either receive the unsubscribe
 * message without any further action or re-subscribe using the
 * {@link io.awspring.cloud.sns.handlers.NotificationStatus#confirmSubscription()} method.
 * <p>
 * A notification controller will be mapped to a particular url inside the application context. The mapped url must be
 * configured inside the Amazon Web Service platform as a subscription. Before receiving any notification itself a
 * controller must confirm the subscription.
 * <p>
 * Since 3.0 Annotation can be used on a method level and provide path mapping. Works like {@link RequestMapping}.
 *
 * @author Agim Emruli
 * @author Matej Nedic
 */
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(headers = "x-amz-sns-message-type=UnsubscribeConfirmation", method = RequestMethod.POST)
@ResponseStatus(HttpStatus.NO_CONTENT)
public @interface NotificationUnsubscribeConfirmationMapping {

	@AliasFor(annotation = RequestMapping.class, attribute = "path")
	String[] path() default {};

}
