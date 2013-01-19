/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * AWS backed integration test for the data source feature of the jdbc module
 *
 * @author Agim Emruli
 * @since 1.0
 */
@ContextConfiguration("DataSourceFactoryBeanAwsTest-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class DataSourceFactoryBeanAwsTest {

	@Autowired
	private DatabaseService databaseService;

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testExistingDataSourceInstance() throws Exception {
		Assert.assertTrue(this.databaseService.checkDatabase());
	}
}
