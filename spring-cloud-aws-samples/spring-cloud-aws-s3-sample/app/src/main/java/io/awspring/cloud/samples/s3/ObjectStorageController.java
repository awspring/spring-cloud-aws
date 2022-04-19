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

import io.awspring.cloud.s3.S3Template;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stores regular Java objects as S3 files.
 */
@RestController
@RequestMapping("/object")
public class ObjectStorageController {
	private static final String BUCKET = "spring-cloud-aws-sample-bucket1";

	private final S3Template s3Template;

	public ObjectStorageController(S3Template s3Template) {
		this.s3Template = s3Template;
	}

	@PostMapping
	void store(@RequestBody Person p) {
		s3Template.store(BUCKET, p.id + ".json", p);
	}

	@GetMapping("/{id}")
	Person read(@PathVariable Long id) {
		return s3Template.read(BUCKET, id + ".json", Person.class);
	}

	static class Person {
		private Long id;
		private String firstName;
		private String lastName;

		public Person() {
		}

		public Person(Long id, String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
	}
}
