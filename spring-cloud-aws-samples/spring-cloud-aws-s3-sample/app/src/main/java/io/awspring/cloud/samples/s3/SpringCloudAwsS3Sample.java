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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootApplication
public class SpringCloudAwsS3Sample {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudAwsS3Sample.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudAwsS3Sample.class, args);
	}

	// load resource using @Value
	@Value("s3://spring-cloud-aws-sample-bucket1/test-file.txt")
	private Resource file;

	@Bean
	ApplicationRunner applicationRunner(S3Client s3Client, ResourceLoader resourceLoader) {
		return args -> {
			// use auto-configured cross-region client
			s3Client.listObjects(request -> request.bucket("spring-cloud-aws-sample-bucket1")).contents()
					.forEach(s3Object -> LOGGER.info("Object in bucket: {}", s3Object.key()));

			// load resource using ResourceLoader
			WritableResource resource = (WritableResource) resourceLoader
					.getResource("s3://spring-cloud-aws-sample-bucket1/my-file.txt");
			LOGGER.info("File content: {}", readContent(resource));

			// load content of file retrieved with @Value
			LOGGER.info("File content: {}", readContent(file));

			// write to resource
			try (OutputStream outputStream = resource.getOutputStream()) {
				outputStream.write("overwritten".getBytes(StandardCharsets.UTF_8));
			}
			LOGGER.info("Overwritten content: {}", readContent(resource));
		};
	}

	private String readContent(Resource resource) throws IOException {
		Scanner s = new Scanner(resource.getInputStream()).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

}
