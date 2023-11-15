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
package io.awspring.cloud.autoconfigure.s3;

import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption;
import software.amazon.awssdk.transfer.s3.internal.TransferManagerConfiguration;

/**
 * Exposes internals of {@link S3TransferManager} through reflection to simplify testing.
 *
 * @author Maciej Walkowiak
 */
public class ConfiguredTransferManager {
	private final TransferManagerConfiguration transferConfiguration;
	private final S3AsyncClient client;

	ConfiguredTransferManager(S3TransferManager s3TransferManager) {
		// todo: this is getting too hacky
		if (s3TransferManager.getClass().getName()
				.equals("software.amazon.awssdk.transfer.s3.internal.CrtS3TransferManager")) {
			S3TransferManager delegate = (S3TransferManager) ReflectionTestUtils.getField(s3TransferManager,
					"delegate");
			this.transferConfiguration = (TransferManagerConfiguration) ReflectionTestUtils.getField(delegate,
					"transferConfiguration");
			this.client = (S3AsyncClient) ReflectionTestUtils.getField(s3TransferManager, "s3AsyncClient");
		}
		else {
			this.transferConfiguration = (TransferManagerConfiguration) ReflectionTestUtils.getField(s3TransferManager,
					"transferConfiguration");
			this.client = (S3AsyncClient) ReflectionTestUtils.getField(s3TransferManager, "s3AsyncClient");
		}

	}

	boolean getUploadDirectoryFileVisitOption() {
		return transferConfiguration.option(TransferConfigurationOption.UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS);
	}

	Integer getMaxDepth() {
		return transferConfiguration.option(TransferConfigurationOption.UPLOAD_DIRECTORY_MAX_DEPTH);
	}

	S3AsyncClient getClient() {
		return client;
	}
}
