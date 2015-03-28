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

package org.springframework.cloud.aws.core.config;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;

/**
 * {@link org.springframework.beans.factory.FactoryBean} class to create {@link AmazonWebServiceClient} instances. This class
 * is responsible to create the respective AmazonWebServiceClient classes because the configuration through Springs's
 * BeanFactory fails due to invalid properties inside the Webservice client classes (see
 * https://github.com/aws/aws-sdk-java/issues/325)
 *
 * @author Agim Emruli
 */
public class AmazonWebserviceClientFactoryBean<T extends AmazonWebServiceClient> extends AbstractFactoryBean<T> {

	private final Class<? extends AmazonWebServiceClient> clientClass;
	private final AWSCredentialsProvider credentialsProvider;
	private RegionProvider regionProvider;
	private Region customRegion;
	private ExecutorService executor;

	public AmazonWebserviceClientFactoryBean(Class<T> clientClass,
											 AWSCredentialsProvider credentialsProvider) {
		this.clientClass = clientClass;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public Class<?> getObjectType() {
		return this.clientClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T createInstance() throws Exception {

		T webServiceClient;

		if (this.executor != null) {
			Constructor<? extends AmazonWebServiceClient> constructor = ClassUtils.
					getConstructorIfAvailable(this.clientClass, AWSCredentialsProvider.class, ExecutorService.class);
			Assert.notNull(constructor);

			webServiceClient =
					(T) BeanUtils.instantiateClass(constructor, this.credentialsProvider, this.executor);
		} else {
			Constructor<? extends AmazonWebServiceClient> constructor = ClassUtils.
					getConstructorIfAvailable(this.clientClass, AWSCredentialsProvider.class);
			Assert.notNull(constructor);

			webServiceClient =
					(T) BeanUtils.instantiateClass(constructor, this.credentialsProvider);
		}

		if (this.customRegion != null) {
			webServiceClient.setRegion(this.customRegion);
		} else if (this.regionProvider != null) {
			webServiceClient.setRegion(this.regionProvider.getRegion());
		}
		return webServiceClient;
	}

	public void setRegionProvider(RegionProvider regionProvider) {
		this.regionProvider = regionProvider;
	}

	public void setCustomRegion(String customRegionName) {
		this.customRegion = RegionUtils.getRegion(customRegionName);
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	protected void destroyInstance(T instance) throws Exception {
		instance.shutdown();
	}
}
