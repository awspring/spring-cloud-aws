/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.core.config;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsAsyncClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.ExecutorFactory;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import io.awspring.cloud.core.SpringCloudClientConfiguration;
import io.awspring.cloud.core.region.RegionProvider;
import io.awspring.cloud.core.region.StaticRegionProvider;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} class to create
 * {@link AmazonWebServiceClient} instances. This class is responsible to create the
 * respective AmazonWebServiceClient classes because the configuration through Springs's
 * BeanFactory fails due to invalid properties inside the Webservice client classes (see
 * https://github.com/aws/aws-sdk-java/issues/325)
 *
 * @param <T> implementation of the {@link AmazonWebServiceClient}
 * @author Agim Emruli
 * @author Eddú Meléndez
 */
public class AmazonWebserviceClientFactoryBean<T extends AmazonWebServiceClient> extends AbstractFactoryBean<T> {

	private final Class<? extends AmazonWebServiceClient> clientClass;

	private final AWSCredentialsProvider credentialsProvider;

	private RegionProvider regionProvider;

	private Region customRegion;

	private ExecutorService executor;

	private URI customEndpoint;

	private ClientConfiguration clientConfiguration;

	public AmazonWebserviceClientFactoryBean(Class<T> clientClass, AWSCredentialsProvider credentialsProvider) {
		this.clientClass = clientClass;
		this.credentialsProvider = credentialsProvider;
	}

	public AmazonWebserviceClientFactoryBean(Class<T> clientClass, AWSCredentialsProvider credentialsProvider,
			RegionProvider regionProvider) {
		this(clientClass, credentialsProvider);
		setRegionProvider(regionProvider);
	}

	public AmazonWebserviceClientFactoryBean(Class<T> clientClass, AWSCredentialsProvider credentialsProvider,
			RegionProvider regionProvider, ClientConfiguration clientConfiguration) {
		this(clientClass, credentialsProvider);
		setRegionProvider(regionProvider);
		setClientConfiguration(clientConfiguration);
	}

	@Override
	public Class<?> getObjectType() {
		return this.clientClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T createInstance() throws Exception {

		String builderName = this.clientClass.getName() + "Builder";
		Class<?> className = ClassUtils.resolveClassName(builderName, ClassUtils.getDefaultClassLoader());

		Method method = ClassUtils.getStaticMethod(className, "standard");
		Assert.notNull(method, "Could not find standard() method in class:'" + className.getName() + "'");

		AwsClientBuilder<?, T> builder = (AwsClientBuilder<?, T>) ReflectionUtils.invokeMethod(method, null);

		if (this.executor != null) {
			AwsAsyncClientBuilder<?, T> asyncBuilder = (AwsAsyncClientBuilder<?, T>) builder;
			asyncBuilder.withExecutorFactory((ExecutorFactory) () -> this.executor);
		}

		builder.withClientConfiguration(SpringCloudClientConfiguration.getClientConfiguration(clientConfiguration));

		if (this.credentialsProvider != null) {
			builder.withCredentials(this.credentialsProvider);
		}

		// endpoint can be overwritten when used with AWS-compatible environment
		if (this.customEndpoint != null) {
			// if in addition to custom endpoint, a region was explicitly configured, it
			// must be set as a signing region
			// for compatibility with multi-region AWS-compatible environments like
			// Localstack
			// NOTE: we consider only static region as a valid signing region because of
			// the backward compatibility
			builder.withEndpointConfiguration(
					new AwsClientBuilder.EndpointConfiguration(this.customEndpoint.toString(), findStaticRegion()));
		}
		else {
			if (this.customRegion != null) {
				builder.withRegion(this.customRegion.getName());
			}
			else if (this.regionProvider != null) {
				builder.withRegion(this.regionProvider.getRegion().getName());
			}
			else {
				builder.withRegion(Regions.DEFAULT_REGION);
			}
		}
		return builder.build();
	}

	public void setRegionProvider(RegionProvider regionProvider) {
		this.regionProvider = regionProvider;
	}

	public void setCustomRegion(String customRegionName) {
		this.customRegion = RegionUtils.getRegion(customRegionName);
	}

	public void setCustomEndpoint(URI customEndpoint) {
		this.customEndpoint = customEndpoint;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public void setClientConfiguration(ClientConfiguration clientConfiguration) {
		this.clientConfiguration = clientConfiguration;
	}

	@Override
	protected void destroyInstance(T instance) throws Exception {
		instance.shutdown();
	}

	private String findStaticRegion() {
		if (this.regionProvider != null && this.regionProvider instanceof StaticRegionProvider) {
			return this.regionProvider.getRegion().getName();
		}
		else {
			return null;
		}
	}

}
