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
package io.awspring.cloud.s3;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Meta-annotation for {@link S3ResourceIntegrationTests}.
 *
 * Used to annotate the tests so they run Parametrized based on a different implementation of the
 * {@link S3OutputStreamProvider}. See
 * <a href= "https://junit.org/junit5/docs/current/user-guide/#writing-tests-meta-annotations">
 * https://junit.org/junit5/docs/current/user-guide/#writing-tests-meta-annotations </a>
 *
 * @author Anton Perez
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ParameterizedTest
@MethodSource("availableS3OutputStreamProviders")
public @interface S3ResourceIntegrationTest {
}
