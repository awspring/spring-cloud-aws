/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.samples.s3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.UploadFileRequest;

/**
 * An example on how to upload and download files using TransferManager
 */
@RestController
@RequestMapping("/file/transfermanager")
public class TransferManagerFileController {
	private static final String BUCKET = "spring-cloud-aws-sample-bucket1";
	private final S3TransferManager transferManager;
	private static final String TEMPORARY_PATH_STRING_FORMAT = "/tmp/%s";

	public TransferManagerFileController(S3TransferManager transferManager) {
		this.transferManager = transferManager;
	}

	@PostMapping
	ResponseEntity<String> upload(@RequestPart(name = "file", required = false) MultipartFile multipartFile)
			throws IOException {
		String key = multipartFile.getOriginalFilename();
		File tempFile = new File(String.format(TEMPORARY_PATH_STRING_FORMAT, key));
		try (OutputStream outputStream = new FileOutputStream(tempFile)) {
			IOUtils.copy(multipartFile.getInputStream(), outputStream);
		}
		PutObjectRequest putObjectRequest = PutObjectRequest.builder().key(key).bucket(BUCKET).build();
		transferManager
				.uploadFile(UploadFileRequest.builder().source(tempFile).putObjectRequest(putObjectRequest).build());
		return ResponseEntity.accepted().body(tempFile.getPath());
	}

	@GetMapping
	ResponseEntity<String> download(@RequestParam String key) {
		File tempFile = new File(String.format(TEMPORARY_PATH_STRING_FORMAT, key));
		transferManager
				.downloadFile(b -> b.destination(tempFile)
						.getObjectRequest(GetObjectRequest.builder().bucket(BUCKET).key(key).build()).build())
				.completionFuture().join();
		return ResponseEntity.ok().body(tempFile.getPath());
	}
}
