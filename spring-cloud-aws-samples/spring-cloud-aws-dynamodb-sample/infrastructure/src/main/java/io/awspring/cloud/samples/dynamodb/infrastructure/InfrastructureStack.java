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
package io.awspring.cloud.samples.dynamodb.infrastructure;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.constructs.Construct;

public class InfrastructureStack extends Stack {

	public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
		super(scope, id, props);

		TableProps tableProps = TableProps.builder().tableName("department")
				.partitionKey(Attribute.builder().name("departmentId").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("userId").type(AttributeType.STRING).build()).build();
		new Table(this, "department", tableProps);
	}
}
