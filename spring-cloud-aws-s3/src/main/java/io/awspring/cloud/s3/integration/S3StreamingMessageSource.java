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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.metadata.SimpleMetadataStore;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A {@link AbstractRemoteFileStreamingMessageSource} implementation for the Amazon S3.
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class S3StreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<S3Object> {

	public S3StreamingMessageSource(RemoteFileTemplate<S3Object> template) {
		super(template, null);
	}

	@SuppressWarnings("this-escape")
	public S3StreamingMessageSource(RemoteFileTemplate<S3Object> template, Comparator<S3Object> comparator) {
		super(template, comparator);
		doSetFilter(new S3PersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "s3StreamingMessageSource"));
	}

	@Override
	protected List<AbstractFileInfo<S3Object>> asFileInfoList(Collection<S3Object> collection) {
		return collection.stream().map(S3FileInfo::new).collect(Collectors.toList());
	}

	@Override
	public String getComponentType() {
		return "aws:s3-inbound-streaming-channel-adapter";
	}

	@Override
	protected AbstractFileInfo<S3Object> poll() {
		AbstractFileInfo<S3Object> file = super.poll();
		if (file != null) {
			S3Session s3Session = (S3Session) getRemoteFileTemplate().getSession();
			file.setRemoteDirectory(s3Session.normalizeBucketName(file.getRemoteDirectory()));
		}
		return file;
	}

	@Override
	protected boolean isDirectory(S3Object file) {
		return false;
	}

}
