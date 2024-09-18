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
package io.awspring.cloud.autoconfigure.imds;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.imds.Ec2MetadataClient;

public class ImdsAutoConfigurationIntegrationTest {

	private WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());
	private ImdsUtils imdsUtils;

	@BeforeEach
	void init() {
		wireMockServer.start();
		imdsUtils = new ImdsUtils(Ec2MetadataClient.builder().endpoint(URI.create(wireMockServer.baseUrl())).build());
	}

	@AfterEach
	void stop() {
		wireMockServer.stop();
	}

	/**
	 * If the IMDS is not available, no exceptions should occur; the metadata should simply not be available.
	 */
	@Test
	void imdsNotAvailable() {
		wireMockServer.stop();
		assertThat(imdsUtils.isRunningOnCloudEnvironment()).isFalse();
	}

	/**
	 * If IMDS is available, the utility object should report that we are running within an EC2-based environment.
	 */
	@Test
	void imdsIsCloud() {
		addStubToken();
		wireMockServer.stubFor(
				get(urlEqualTo("/latest/meta-data/ami-id")).willReturn(responseDefinition().withBody("sample-ami-id")));

		assertThat(imdsUtils.isRunningOnCloudEnvironment()).isTrue();
	}

	/**
	 * Test retrieval of a subset of IMDS data.
	 */
	@Test
	void imdsIsDataRetrieved() {
		addStubToken();
		wireMockServer.stubFor(
				get(urlEqualTo("/latest/meta-data/ami-id")).willReturn(responseDefinition().withBody("sample-ami-id")));
		wireMockServer.stubFor(
				get(urlEqualTo("/latest/meta-data/public-ipv4")).willReturn(responseDefinition().withBody("1.2.3.4")));
		wireMockServer.stubFor(get(urlEqualTo("/latest/meta-data/placement/partition-number"))
				.willReturn(responseDefinition().withBody("2")));

		Map<String, String> map = imdsUtils.getEc2InstanceMetadata();

		assertThat(map.get("ami-id")).isEqualTo("sample-ami-id");
		assertThat(map.get("public-ipv4")).isEqualTo("1.2.3.4");
		assertThat(map.get("placement/partition-number")).isEqualTo("2");
	}

	private void addStubToken() {
		wireMockServer.stubFor(put(urlEqualTo("/latest/api/token")).willReturn(
				responseDefinition().withHeader("x-aws-ec2-metadata-token-ttl-seconds", "100").withBody("a-token")));

	}
}
