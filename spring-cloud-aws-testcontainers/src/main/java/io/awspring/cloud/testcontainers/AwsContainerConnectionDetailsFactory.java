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
package io.awspring.cloud.testcontainers;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import java.net.URI;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.localstack.LocalStackContainer;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link AwsConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link LocalStackContainer}.
 *
 * @author Maciej Walkowiak
 * @since 3.2.0
 */
public class AwsContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<LocalStackContainer, AwsConnectionDetails> {
	@Override
	protected AwsConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<LocalStackContainer> source) {
		return new AwsContainerConnectionDetails(source);
	}

	private static final class AwsContainerConnectionDetails extends ContainerConnectionDetails<LocalStackContainer>
			implements AwsConnectionDetails {

		protected AwsContainerConnectionDetails(ContainerConnectionSource<LocalStackContainer> source) {
			super(source);
		}

		@Override
		public URI getEndpoint() {
			return getContainer().getEndpoint();
		}

		@Override
		public String getRegion() {
			return getContainer().getRegion();
		}

		@Override
		public String getAccessKey() {
			return getContainer().getAccessKey();
		}

		@Override
		public String getSecretKey() {
			return getContainer().getSecretKey();
		}
	}
}
