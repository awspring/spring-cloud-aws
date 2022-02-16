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

package io.awspring.cloud.test.sqs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Annotation for a SQS test that focuses <strong>only</strong> on SQS-based components.
 * <p>
 * Using this annotation will disable full auto-configuration and instead apply only
 * configuration relevant to SQS tests.
 * <p>
 * When using JUnit 4, this annotation should be used in combination with
 * {@code @RunWith(SpringRunner.class)}.
 *
 * @author Maciej Walkowiak
 * @since 2.4.0
 * @see AutoConfigureSqs
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(SqsTestContextBootstrapper.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(SqsTypeExcludeFilter.class)
@ExtendWith(SpringExtension.class)
@AutoConfigureSqs
@ImportAutoConfiguration
public @interface SqsTest {

	/**
	 * Determines if default filtering should be used with
	 * {@link SpringBootApplication @SpringBootApplication}. By default no beans are
	 * included.
	 * @see #includeFilters()
	 * @see #excludeFilters()
	 * @return if default filters should be used
	 */
	boolean useDefaultFilters() default true;

	/**
	 * Specifies the listeners to test. This is an alias of {@link #listeners()} which can
	 * be used for brevity if no other attributes are defined. See {@link #listeners()}
	 * for details.
	 * @see #listeners()
	 * @return the listeners to test
	 */
	@AliasFor("listeners")
	Class<?>[] value() default {};

	/**
	 * Specifies the listeners to test.
	 * @see #value()
	 * @return the listeners to test
	 */
	@AliasFor("value")
	Class<?>[] listeners() default {};

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
	 */
	String[] properties() default {};

	/**
	 * A set of include filters which can be used to add otherwise filtered beans to the
	 * application context.
	 * @return include filters to apply
	 */
	ComponentScan.Filter[] includeFilters() default {};

	/**
	 * A set of exclude filters which can be used to filter beans that would otherwise be
	 * added to the application context.
	 * @return exclude filters to apply
	 */
	ComponentScan.Filter[] excludeFilters() default {};

}
