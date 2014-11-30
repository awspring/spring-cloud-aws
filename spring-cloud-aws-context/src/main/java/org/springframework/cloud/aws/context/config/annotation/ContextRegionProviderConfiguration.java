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

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.regions.Regions;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.core.config.support.ContextAnnotationConfigUtil;
import org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 */
@Configuration
public class ContextRegionProviderConfiguration implements ImportAware {

	private AnnotationAttributes annotationAttributes;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationAttributes = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableRegionProvider.class.getName(), false));
		Assert.notNull(this.annotationAttributes,
				"@EnableRegionProvider is not present on importing class " + importMetadata.getClassName());
	}

	@Bean(name = {AmazonWebserviceClientConfigurationUtils.REGION_PROVIDER_BEAN_NAME, "regionProvider"})
	public RegionProvider regionProvider(ConfigurableBeanFactory configurableBeanFactory) {
		boolean autoDetect = this.annotationAttributes.getBoolean("autoDetect");
		String configuredRegion = this.annotationAttributes.getString("region");

		if (autoDetect
				&& StringUtils.hasText(configuredRegion)) {
			throw new IllegalArgumentException("No region must be configured if autoDetect is defined as true");
		}

		if (autoDetect) {
			return new Ec2MetadataRegionProvider();
		}

		if (StringUtils.hasText(configuredRegion)) {
			String resolvedRegion = ContextAnnotationConfigUtil.resolveStringValue(configurableBeanFactory,
					configuredRegion);
			return new StaticRegionProvider(Regions.valueOf(resolvedRegion));
		}

		throw new IllegalArgumentException("Region must be manually configured or autoDetect enabled");
	}
}
