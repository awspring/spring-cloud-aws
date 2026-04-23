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
import io.floci.testcontainers.FlociContainer;
import java.net.URI;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link AwsConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link FlociContainer}.
 *
 * @author Bastian Hellmann
 * @since 4.1.0
 */
public class AwsFlociContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<FlociContainer, AwsConnectionDetails> {
	@Override
	protected AwsConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<FlociContainer> source) {
		return new AwsContainerConnectionDetails(source);
	}

	private static final class AwsContainerConnectionDetails extends ContainerConnectionDetails<FlociContainer>
			implements AwsConnectionDetails {

		protected AwsContainerConnectionDetails(ContainerConnectionSource<FlociContainer> source) {
			super(source);
		}

		@Override
		public URI getEndpoint() {
			return URI.create(getContainer().getEndpoint());
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
