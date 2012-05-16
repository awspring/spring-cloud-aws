/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class SimpleStorageResource extends AbstractResource implements WritableResource {

	private final String bucketName;
	private final String objectName;
	private final AmazonS3 amazonS3;
	private final ObjectMetadata objectMetadata;

	public SimpleStorageResource(String bucketName, String objectName, AmazonS3 amazonS3) {
		this.bucketName = bucketName;
		this.objectName = objectName;
		this.amazonS3 = amazonS3;
		this.objectMetadata = amazonS3.getObjectMetadata(bucketName, objectName);
	}

	public String getDescription() {
		StringBuilder builder = new StringBuilder("Amazon s3 resource [bucket='");
		builder.append(this.bucketName);
		builder.append("' and object='");
		builder.append(this.objectName);
		builder.append("']");
		return builder.toString();
	}

	public InputStream getInputStream() throws IOException {
		return this.amazonS3.getObject(this.bucketName, this.objectName).getObjectContent();
	}

	@Override
	public long lastModified() throws IOException {
		assertThatResourceExists();
		return this.objectMetadata.getLastModified().getTime();
	}

	@Override
	public long contentLength() throws IOException {
		assertThatResourceExists();
		return this.objectMetadata.getContentLength();
	}

	@Override
	public boolean exists() {
		return this.objectMetadata != null;
	}

	@Override
	public String getFilename() throws IllegalStateException {
		return this.objectName;
	}

	private void assertThatResourceExists() throws FileNotFoundException {
		if (this.objectMetadata == null) {
			throw new FileNotFoundException(new StringBuilder().
					append("Resource with bucket='").
					append(this.bucketName).
					append("' and objectName='").
					append(this.objectName).
					append("' not found!").
					toString());
		}
	}

	@Override
	public boolean isWritable() {
		return this.objectMetadata != null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new DecoratingOutputStream();
	}


	private class DecoratingOutputStream extends OutputStream {

		private final OutputStream tempFileOutputStream;
		private final File tempFile;

		private DecoratingOutputStream() {
			try {
				this.tempFile = File.createTempFile(SimpleStorageResource.this.bucketName, SimpleStorageResource.this.objectName);
				this.tempFile.deleteOnExit();
				this.tempFileOutputStream = new FileOutputStream(this.tempFile);
			} catch (IOException e) {
				throw new RuntimeException("Error creating temporary file ", e);
			}
		}

		@Override
		public void write(int b) throws IOException {
			this.tempFileOutputStream.write(b);
		}

		@Override
		public void flush() throws IOException {
			this.tempFileOutputStream.flush();
			this.tempFileOutputStream.close();
			PutObjectResult result = SimpleStorageResource.this.amazonS3.putObject(SimpleStorageResource.this.bucketName,
					SimpleStorageResource.this.objectName, new FileInputStream(this.tempFile),
					SimpleStorageResource.this.objectMetadata);
			if(result == null){
				throw new IOException("Error uploading s3 resource" + SimpleStorageResource.this.objectName);
			}

		}

		@Override
		public void close() throws IOException {
			this.flush();
		}
	}
}