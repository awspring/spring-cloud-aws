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
package io.awspring.cloud.autoconfigure.sesv2;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

/**
 * Callback interface that can be used to customize a {@link SesV2ClientBuilder}.
 *
 * @author Maciej Walkowiak
 * @since 3.3.0
 */
@FunctionalInterface
public interface SesV2ClientCustomizer extends AwsClientCustomizer<SesV2ClientBuilder> {
}
