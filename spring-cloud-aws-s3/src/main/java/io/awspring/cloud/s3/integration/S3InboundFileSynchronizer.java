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

import java.io.File;
import java.io.IOException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * An implementation of {@link AbstractInboundFileSynchronizer} for Amazon S3.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class S3InboundFileSynchronizer extends AbstractInboundFileSynchronizer<S3Object> {

	public S3InboundFileSynchronizer() {
		this(new S3SessionFactory());
	}

	public S3InboundFileSynchronizer(S3Client amazonS3) {
		this(new S3SessionFactory(amazonS3));
	}

	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire {@link Session} instances.
	 * @param sessionFactory The session factory.
	 */
	@SuppressWarnings("this-escape")
	public S3InboundFileSynchronizer(SessionFactory<S3Object> sessionFactory) {
		super(sessionFactory);
		doSetRemoteDirectoryExpression(new LiteralExpression(null));
		doSetFilter(new S3PersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "s3MessageSource"));
	}

	@Override
	protected boolean isFile(S3Object file) {
		return true;
	}

	@Override
	protected String getFilename(S3Object file) {
		return (file != null ? file.key() : null);
	}

	@Override
	protected long getModified(S3Object file) {
		return file.lastModified().getEpochSecond();
	}

	@Override
	protected boolean copyFileToLocalDirectory(String remoteDirectoryPath,
			@Nullable EvaluationContext localFileEvaluationContext, S3Object remoteFile, File localDirectory,
			Session<S3Object> session) throws IOException {

		return super.copyFileToLocalDirectory(((S3Session) session).normalizeBucketName(remoteDirectoryPath),
				localFileEvaluationContext, remoteFile, localDirectory, session);
	}

	@Override
	protected String protocol() {
		return "s3";
	}

}
