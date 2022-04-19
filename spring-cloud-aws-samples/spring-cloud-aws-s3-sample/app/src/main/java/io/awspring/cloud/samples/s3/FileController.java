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

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * An example on how to create file upload to S3.
 */
@RestController
@RequestMapping("/file")
public class FileController {
	private static final String BUCKET = "spring-cloud-aws-sample-bucket1";
	private final S3Operations s3Operations;

	public FileController(S3Operations s3Operations) {
		this.s3Operations = s3Operations;
	}

	@PostMapping
	ResponseEntity<?> upload(MultipartFile multipartFile) throws IOException {
		if (!MediaType.IMAGE_PNG.toString().equals(multipartFile.getContentType())) {
			return ResponseEntity.badRequest().body("only png images are allowed");
		}
		try (InputStream is = multipartFile.getInputStream()) {
			s3Operations.upload(BUCKET, multipartFile.getOriginalFilename(), is,
					ObjectMetadata.builder().contentType(multipartFile.getContentType()).build());
		}
		return ResponseEntity.accepted().build();
	}

	@GetMapping
	ResponseEntity<Resource> download(@RequestParam String key) {
		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(s3Operations.download(BUCKET, key));
	}
}
