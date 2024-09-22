/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.docker.compose;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import java.net.URI;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link AwsConnectionDetails} for a {@code localstack}
 * service.
 *
 * @author Dominik Kovács
 * @since 3.2.0
 */
class AwsDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<AwsConnectionDetails> {

	private static final String[] LOCALSTACK_CONTAINER_NAMES = { "localstack/localstack", "localstack/localstack-pro" };

	private static final int LOCALSTACK_PORT = 4566;

	AwsDockerComposeConnectionDetailsFactory() {
		super(LOCALSTACK_CONTAINER_NAMES);
	}

	@Override
	protected AwsConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new AwsDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link DockerComposeConnectionDetails} backed by a {@code localstack} {@link RunningService}.
	 */
	private static final class AwsDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements AwsConnectionDetails {

		private final LocalStackEnvironment environment;

		private final URI endpoint;

		private AwsDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new LocalStackEnvironment(service.env());
			this.endpoint = URI.create("http://%s:%s".formatted(service.host(), service.ports().get(LOCALSTACK_PORT)));
		}

		@Override
		public URI getEndpoint() {
			return this.endpoint;
		}

		@Override
		public String getRegion() {
			return this.environment.getRegion();
		}

		@Override
		public String getAccessKey() {
			return this.environment.getAccessKey();
		}

		@Override
		public String getSecretKey() {
			return this.environment.getSecretKey();
		}
	}
}
