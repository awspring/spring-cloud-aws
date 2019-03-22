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
 * Configures a {@link org.springframework.cloud.aws.core.region.RegionProvider} instance
 * for the application context. The region provider will be used for all Amazon Web
 * Service clients that are created inside the application context (by the Spring Cloud
 * AWS classes). A region can be either manually configured
 * {@link EnableContextRegion#region()} with a constant expression, dynamic expression
 * (using a SpEL expression) or a place holder. The region can also be dynamically
 * retrieved from the EC2 instance meta-data if the application context is running inside
 * a EC2 instance by enabling the {@link EnableContextRegion#autoDetect()} attribute.
 *
 * @author Agim Emruli
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(ContextRegionConfigurationRegistrar.class)
public @interface EnableContextRegion {

	/**
	 * Configures the region as a String value. The value must match to an enum defined in
	 * {@link com.amazonaws.regions.Regions}. This attribute is a String value allowing
	 * expressions and placeholders to be used for the region configuration.
	 * @return - the region a constant definition, SpEL expression or placeholder
	 * definition
	 */
	String region() default "";

	/**
	 * Configures the auto-detection of a region that should be fetched from the EC2
	 * meta-data. Disabled by default.
	 * @return - configures if the region should be fetched by the EC2 meta-data. Must be
	 * false if a region is configured in the {@link EnableContextRegion#region()}
	 * attribute.
	 */
	boolean autoDetect() default false;

}
