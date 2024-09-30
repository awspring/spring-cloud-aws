/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.autoconfigure;

import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;

/**
 * Callback interface that can be used to customize a {@link AwsSyncClientBuilder}.
 * <p>
 * It gets applied to every configured synchronous AWS client bean.
 *
 * @author Maciej Walkowiak
 * @since 3.3.0
 */
public interface AwsSyncClientCustomizer {
	/**
	 * Callback to customize a {@link AwsSyncClientBuilder} instance.
	 *
	 * @param builder the client builder to customize
	 */
	void customize(AwsSyncClientBuilder<?, ?> builder);
}
