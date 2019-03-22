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
 * Represents a registry of logical stack resource ids mapped to physical resource ids.
 *
 * @author Christian Stettler
 */
public interface StackResourceRegistry {

	/**
	 * Returns the name of the stack represented by this stack resource registry.
	 * @return the name of the stack
	 */
	String getStackName();

	/**
	 * Returns the physical id of the resource identified by the provided logical resource
	 * id. If no resource with the provided logical id exists, null is returned.
	 * @param logicalResourceId the logical id of the resource
	 * @return the physical id of the resource, or null, if no resource for the logical id
	 * exists in this stack.
	 */
	String lookupPhysicalResourceId(String logicalResourceId);

}
