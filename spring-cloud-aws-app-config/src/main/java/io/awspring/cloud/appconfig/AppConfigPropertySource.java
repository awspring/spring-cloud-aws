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
package io.awspring.cloud.appconfig;

import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

public class AppConfigPropertySource extends EnumerablePropertySource<AppConfigDataClient> {

	// logger must stay static non-final so that it can be set with a value in
	// AppConfigDataLoader
	private static Log LOG = LogFactory.getLog(AppConfigPropertySource.class);

	private final String configurationProfileIdentifier;
	private final String environmentIdentifier;
	private final String applicationIdentifier;
	private Properties properties;

	public AppConfigPropertySource(RequestContext requestContext, AppConfigDataClient appConfigDataClient) {
		super(requestContext.getContext(), appConfigDataClient);
		this.configurationProfileIdentifier = requestContext.getConfigurationProfileIdentifier();
		this.environmentIdentifier = requestContext.getEnvironmentIdentifier();
		this.applicationIdentifier = requestContext.getApplicationIdentifier();

	}

	public void init() {
		StartConfigurationSessionRequest sessionRequest = StartConfigurationSessionRequest.builder()
				.environmentIdentifier(this.environmentIdentifier).applicationIdentifier(this.applicationIdentifier)
				.configurationProfileIdentifier(this.configurationProfileIdentifier).build();
		StartConfigurationSessionResponse response = this.source.startConfigurationSession(sessionRequest);
		GetLatestConfigurationRequest request = GetLatestConfigurationRequest.builder()
				.configurationToken(response.initialConfigurationToken()).build();
		getParameters(request);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = this.properties.stringPropertyNames();
		return strings.toArray(new String[strings.size()]);
	}

	@Override
	@Nullable
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	private void getParameters(GetLatestConfigurationRequest request) {
		GetLatestConfigurationResponse response = this.source.getLatestConfiguration(request);
		YamlPropertiesFactoryBean bean = new YamlPropertiesFactoryBean();
		bean.setResources(new ByteArrayResource(response.configuration().asByteArray()));
		properties = bean.getObject();
	}

}
