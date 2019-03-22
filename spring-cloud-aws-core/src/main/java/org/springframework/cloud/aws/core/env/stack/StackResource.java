/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.core.env.stack;

/**
 * @author Agim Emruli
 */
public class StackResource {

	private final String logicalId;

	private final String physicalId;

	private final String type;

	public StackResource(String logicalId, String physicalId, String type) {
		this.logicalId = logicalId;
		this.physicalId = physicalId;
		this.type = type;
	}

	public String getLogicalId() {
		return this.logicalId;
	}

	public String getPhysicalId() {
		return this.physicalId;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("StackResource{");
		sb.append("logicalId='").append(this.logicalId).append("\'");
		sb.append(", physicalId='").append(this.physicalId).append("\'");
		sb.append(", type='").append(this.type).append("\'");
		sb.append("}");
		return sb.toString();
	}

}
