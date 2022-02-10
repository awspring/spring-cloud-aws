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

package io.awspring.cloud.v3.it.core.env.ec2;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.awspring.cloud.context.support.env.AwsCloudEnvironmentCheckUtils;
import io.awspring.cloud.v3.it.LocalStackIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.core.SdkSystemSetting;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 */
public abstract class AmazonEc2InstanceDataPropertySourceAwsTest extends LocalStackIntegrationTest {

	@Autowired
	private SimpleConfigurationBean simpleConfigurationBean;

	@Autowired
	private WireMockServer wireMock;

	@BeforeEach
	public void setupHttpServer() throws Exception {
		overwriteMetadataEndpointUrl(wireMock.baseUrl());

		wireMock.stubFor(get(urlEqualTo("/latest/user-data"))
			.willReturn(ok("key1:value1;key2:value2;key3:value3")));

		wireMock.stubFor(get(urlEqualTo("/latest/meta-data/instance-id"))
			.willReturn(ok("i123456")));

		restContextInstanceDataCondition();
	}

	@AfterEach
	public void tearDown() {
		resetMetadataEndpointUrlOverwrite();
	}

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
				localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
	}

	private static void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(AwsCloudEnvironmentCheckUtils.class, "isCloudEnvironment");
		assertThat(field).isNotNull();
		ReflectionUtils.makeAccessible(field);
		field.set(null, null);
	}

	@Test
	void testInstanceDataResolution() throws Exception {
		assertThat(this.simpleConfigurationBean.getValue1()).isEqualTo("value1");
		assertThat(this.simpleConfigurationBean.getValue2()).isEqualTo("value2");
		assertThat(this.simpleConfigurationBean.getValue3()).isEqualTo("value3");
		assertThat(this.simpleConfigurationBean.getValue4()).isEqualTo("i123456");
	}
}
