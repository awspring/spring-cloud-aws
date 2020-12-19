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

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import static org.springframework.cloud.aws.context.config.support.ContextConfigurationUtils.registerRegionProvider;

/**
 * @author Agim Emruli
 * @deprecated use auto-configuration
 */
@Configuration(proxyBeanMethods = false)
@Deprecated
public class ContextRegionConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes
				.fromMap(importingClassMetadata.getAnnotationAttributes(EnableContextRegion.class.getName(), false));
		Assert.notNull(annotationAttributes,
				"@EnableRegionProvider is not present on importing class " + importingClassMetadata.getClassName());

		boolean autoDetect = annotationAttributes.getBoolean("autoDetect");
		boolean useDefaultAwsRegionChain = annotationAttributes.getBoolean("useDefaultAwsRegionChain");
		String configuredRegion = annotationAttributes.getString("region");

		registerRegionProvider(registry, autoDetect, useDefaultAwsRegionChain, configuredRegion);
	}

}
