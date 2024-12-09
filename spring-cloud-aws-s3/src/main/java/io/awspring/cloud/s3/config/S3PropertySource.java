/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.s3.config;

import io.awspring.cloud.core.config.AwsPropertySource;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Retrieves property sources path from the AWS S3 using the provided S3 client.
 *
 * @author Kunal Varpe
 * @author Matej Nedic
 * @since 3.3.0
 */
public class S3PropertySource extends AwsPropertySource<S3PropertySource, S3Client> {

	private static final String YAML_TYPE = "application/x-yaml";
	private static final String YAML_TYPE_ALTERNATIVE = "text/yaml";
	private static final String TEXT_TYPE = "text/plain";
	private static final String JSON_TYPE = "application/json";

	/**
	 * Path contains bucket name and properties files.
	 */
	private final String context;

	/**
	 * S3 Bucket which stores the property files.
	 */
	private final String bucket;

	private final String key;

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public S3PropertySource(String context, S3Client s3Client) {
		super("aws-s3:" + context, s3Client);
		Assert.notNull(context, "context is required");
		this.context = context;
		this.bucket = resolveBucket(context);
		this.key = resolveKey(context);
	}

	/**
	 * Loads the properties from the S3.
	 */
	@Override
	public void init() {
		readPropertySourcesFromS3(GetObjectRequest.builder().bucket(bucket).key(key).build());
	}

	@Override
	public S3PropertySource copy() {
		return new S3PropertySource(this.context, this.source);
	}

	@Override
	public String[] getPropertyNames() {
		return properties.keySet().toArray(String[]::new);
	}

	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}

	private void readPropertySourcesFromS3(GetObjectRequest getObjectRequest) {
		try (ResponseInputStream<GetObjectResponse> s3PropertyFileResponse = source.getObject(getObjectRequest)) {
			if (s3PropertyFileResponse != null) {
				Properties props = switch (s3PropertyFileResponse.response().contentType()) {
				case TEXT_TYPE -> readProperties(s3PropertyFileResponse);
				case YAML_TYPE, YAML_TYPE_ALTERNATIVE, JSON_TYPE -> readYaml(s3PropertyFileResponse);
				default -> throw new IllegalStateException(
						"Cannot parse unknown content type: " + s3PropertyFileResponse.response().contentType());
				};
				for (Map.Entry<Object, Object> entry : props.entrySet()) {
					properties.put(String.valueOf(entry.getKey()), entry.getValue());
				}
			}
		}
		catch (IOException e) {
			logger.error("Exception has happened while trying to close S3InputStream!", e);
			throw new RuntimeException(e);
		}
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

	@Nullable
	private String resolveBucket(String context) {
		int delimitedIndex = context.indexOf("/");
		if (delimitedIndex != -1) {
			return context.substring(0, delimitedIndex);
		}
		return null;
	}

	private String resolveKey(String context) {
		int delimitedIndex = context.indexOf("/") + 1;
		return context.substring(delimitedIndex);
	}
}
