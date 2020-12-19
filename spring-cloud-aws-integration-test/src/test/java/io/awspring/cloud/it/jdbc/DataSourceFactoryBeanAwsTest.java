/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.it.jdbc;

import java.util.Date;

import io.awspring.cloud.it.AWSIntegration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AWS backed integration test for the datasource feature of the jdbc module
 *
 * @author Agim Emruli
 * @since 1.0
 */
@ExtendWith(SpringExtension.class)
@AWSIntegration
abstract class DataSourceFactoryBeanAwsTest {

	@Autowired
	private DatabaseService databaseService;

	@Test
	void testWriteAndReadWithReadReplicaEnabled() throws Exception {
		Date lastAccessDatabase = this.databaseService.updateLastAccessDatabase();
		Date checkDatabase = this.databaseService.getLastUpdate(lastAccessDatabase);
		assertThat(checkDatabase.getTime()).isEqualTo(lastAccessDatabase.getTime());
	}

}
