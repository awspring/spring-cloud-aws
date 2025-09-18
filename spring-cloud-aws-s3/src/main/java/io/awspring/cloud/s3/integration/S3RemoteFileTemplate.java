/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.s3.integration;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * An Amazon S3 specific {@link RemoteFileTemplate} extension.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class S3RemoteFileTemplate extends RemoteFileTemplate<S3Object> {

	public S3RemoteFileTemplate() {
		this(new S3SessionFactory());
	}

	public S3RemoteFileTemplate(S3Client amazonS3) {
		this(new S3SessionFactory(amazonS3));
	}

	/**
	 * Construct a {@link RemoteFileTemplate} with the supplied session factory.
	 * @param sessionFactory the session factory.
	 */
	public S3RemoteFileTemplate(SessionFactory<S3Object> sessionFactory) {
		super(sessionFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, C> T executeWithClient(final ClientCallback<C, T> callback) {
		return callback.doWithClient((C) this.sessionFactory.getSession().getClientInstance());
	}

	@Override
	public boolean exists(final String path) {
		try {
			return this.sessionFactory.getSession().exists(path);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to check the path " + path, ex);
		}
	}

}
