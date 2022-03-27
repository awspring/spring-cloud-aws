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

import edu.colorado.cires.cmg.s3out.S3ClientMultipartUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * Resolves {@link S3Resource} for resources paths starting from s3://. Registers resolver
 * for S3 protocol in {@link ResourceLoader}.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class S3ProtocolResolver implements ProtocolResolver, ResourceLoaderAware, BeanFactoryPostProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(S3ProtocolResolver.class);

	@Nullable
	private S3Client s3Client;

	@Nullable
	private S3ClientMultipartUpload s3ClientMultipartUpload;

	@Nullable
	private BeanFactory beanFactory;

	// for testing
	S3ProtocolResolver(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	public S3ProtocolResolver() {
	}

	@Override
	public Resource resolve(String location, ResourceLoader resourceLoader) {
		S3Client s3Client = getS3Client();
		if (s3Client == null) {
			LOGGER.warn("Could not resolve S3Client. Resource {} could not be resolved", location);
			return null;
		}

		S3ClientMultipartUpload s3ClientMultipartUpload = getS3ClientMultipartUpload();
		if (s3ClientMultipartUpload == null) {
			LOGGER.warn("Could not resolve S3ClientMultipartUpload. Resource {} could not be resolved", location);
			return null;
		}

		return S3Resource.create(location, s3Client, s3ClientMultipartUpload);
	}

	@Nullable
	private S3ClientMultipartUpload getS3ClientMultipartUpload() {
		if (s3ClientMultipartUpload != null) {
			return s3ClientMultipartUpload;
		}
		else if (beanFactory != null) {
			return beanFactory.getBean(S3ClientMultipartUpload.class);
		}
		else {
			return null;
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		if (DefaultResourceLoader.class.isAssignableFrom(resourceLoader.getClass())) {
			((DefaultResourceLoader) resourceLoader).addProtocolResolver(this);
		}
		else {
			LOGGER.warn("The provided delegate resource loader is not an implementation "
					+ "of DefaultResourceLoader. Custom Protocol using s3:// prefix will not be enabled.");
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Nullable
	private S3Client getS3Client() {
		if (s3Client != null) {
			return s3Client;
		}
		else if (beanFactory != null) {
			return beanFactory.getBean(S3Client.class);
		}
		else {
			return null;
		}
	}

}
