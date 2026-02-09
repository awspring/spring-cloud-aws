/*
 * Copyright 2013-2026 the original author or authors.
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

import io.awspring.cloud.core.config.AwsPropertySource;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;

/**
 * Retrieves configuration property sources path from the AWS AppConfig using the provided {@link AppConfigDataClient}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class AppConfigPropertySource extends AwsPropertySource<AppConfigPropertySource, AppConfigDataClient> {
	private static final String YAML_TYPE = "application/x-yaml";
	private static final String YAML_TYPE_ALTERNATIVE = "text/yaml";
	private static final String TEXT_TYPE = "text/plain";
	private static final String JSON_TYPE = "application/json";

	private final RequestContext context;
	private final AppConfigDataClient appConfigClient;
	private String sessionToken;
	private final Map<String, Object> properties;

	public AppConfigPropertySource(RequestContext context, AppConfigDataClient appConfigClient) {
		this(context, appConfigClient, null, null);
	}

	public AppConfigPropertySource(RequestContext context, AppConfigDataClient appConfigClient,
			@Nullable String sessionToken, Map<String, Object> properties) {
		super("aws-appconfig:" + context, appConfigClient);
		Assert.notNull(context, "context is required");
		this.context = context;
		this.appConfigClient = appConfigClient;
		this.sessionToken = sessionToken;
		this.properties = properties != null ? properties : new LinkedHashMap<>();
	}

	@Override
	public void init() {
		if (!StringUtils.hasText(sessionToken)) {
			var request = StartConfigurationSessionRequest.builder()
					.applicationIdentifier(context.getApplicationIdentifier())
					.environmentIdentifier(context.getEnvironmentIdentifier())
					.configurationProfileIdentifier(context.getConfigurationProfileIdentifier()).build();
			sessionToken = appConfigClient.startConfigurationSession(request).initialConfigurationToken();
		}

		GetLatestConfigurationRequest request = GetLatestConfigurationRequest.builder().configurationToken(sessionToken)
				.build();
		GetLatestConfigurationResponse response = this.source.getLatestConfiguration(request);
		if (response.configuration().asByteArray().length > 0) {
			properties.clear();
			var props = switch (response.contentType()) {
			case TEXT_TYPE -> readProperties(response.configuration().asInputStream());
			case YAML_TYPE, YAML_TYPE_ALTERNATIVE, JSON_TYPE -> readYaml(response.configuration().asInputStream());
			default -> throw new IllegalStateException("Cannot parse unknown content type: " + response.contentType());
			};
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				properties.put(String.valueOf(entry.getKey()), entry.getValue());
			}
		}
		sessionToken = response.nextPollConfigurationToken();
	}

	@Override
	public AppConfigPropertySource copy() {
		return new AppConfigPropertySource(context, appConfigClient, sessionToken, new LinkedHashMap<>(properties));
	}

	@Override
	public String[] getPropertyNames() {
		return properties.keySet().toArray(String[]::new);
	}

	@Override
	public @Nullable Object getProperty(String name) {
		return this.properties.get(name);
	}

	private Properties readProperties(InputStream inputStream) {
		Properties properties = new Properties();
		try (InputStream in = inputStream) {
			properties.load(in);
		}
		catch (Exception e) {
			throw new IllegalStateException("Cannot load environment", e);
		}
		return properties;
	}

	private Properties readYaml(InputStream inputStream) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		try (InputStream in = inputStream) {
			yaml.setResources(new InputStreamResource(in));
			return yaml.getObject();
		}
		catch (Exception e) {
			throw new IllegalStateException("Cannot load environment", e);
		}
	}
}
