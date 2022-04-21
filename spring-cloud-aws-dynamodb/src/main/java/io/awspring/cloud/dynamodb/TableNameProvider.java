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
package io.awspring.cloud.dynamodb;

/**
 * Simple interface which entity should implement when using {@link DynamoDBTemplate} save and update.
 * Since {@link software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean} annotation does not support table name it is impossible to know what is a TableName for a given bean.
 *
 * @author Matej Nedic
 */
public interface TableNameProvider {
	String getTableName();
}
