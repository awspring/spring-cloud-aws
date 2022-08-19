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
package io.awspring.cloud.sqs.config;

/**
 * Beans implementing this interface can configure the {@link EndpointRegistrar} instance used to process
 * {@link Endpoint} instances and change general settings for processing all
 * {@link io.awspring.cloud.sqs.annotation.SqsListener} annotations.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.annotation.SqsListenerAnnotationBeanPostProcessor
 */
@FunctionalInterface
public interface SqsListenerCustomizer {

	/**
	 * Configures the {@link EndpointRegistrar} instance that will handle the {@link Endpoint} instances.
	 * @param registrar the registrar instance.
	 */
	void configure(EndpointRegistrar registrar);

}
