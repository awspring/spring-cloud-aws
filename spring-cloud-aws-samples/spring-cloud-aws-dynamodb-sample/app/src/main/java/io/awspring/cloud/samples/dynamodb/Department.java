/*
 * Copyright 2013-2021 the original author or authors.
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
package io.awspring.cloud.samples.dynamodb;

import java.time.LocalDate;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class Department {

	private UUID userId;
	private UUID departmentId;
	private LocalDate openingDate;
	private Long employeeNumber;

	@DynamoDbPartitionKey
	public UUID getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(UUID departmentId) {
		this.departmentId = departmentId;
	}

	@DynamoDbSortKey
	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public LocalDate getOpeningDate() {
		return openingDate;
	}

	public void setOpeningDate(LocalDate openingDate) {
		this.openingDate = openingDate;
	}

	public Long getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(Long employeeNumber) {
		this.employeeNumber = employeeNumber;
	}

	public static final class Builder {
		private UUID userId;
		private UUID departmentId;
		private LocalDate openingDate;
		private Long employeeNumber;

		private Builder() {
		}

		public static Builder aDepartment() {
			return new Builder();
		}

		public Builder withUserId(UUID userId) {
			this.userId = userId;
			return this;
		}

		public Builder withDepartmentId(UUID departmentId) {
			this.departmentId = departmentId;
			return this;
		}

		public Builder withOpeningDate(LocalDate openingDate) {
			this.openingDate = openingDate;
			return this;
		}

		public Builder withEmployeeNumber(Long employeeNumber) {
			this.employeeNumber = employeeNumber;
			return this;
		}

		public Department build() {
			Department department = new Department();
			department.setUserId(userId);
			department.setDepartmentId(departmentId);
			department.setOpeningDate(openingDate);
			department.setEmployeeNumber(employeeNumber);
			return department;
		}
	}
}
