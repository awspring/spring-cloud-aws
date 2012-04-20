/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc;

import org.elasticspring.jdbc.rds.AmazonRDSDataSourceFactoryBean;
import org.elasticspring.jdbc.rds.CommonsDbcpDataSourceFactory;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.IfProfileValue;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

/**
 *
 *
 */
//@RunWith(SpringJUnit4ClassRunner.class)
public class DataSourceFactoryBeanTest {

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testExistingDataSourceInstance() throws Exception {
		Properties properties = new Properties();
		properties.load(new ClassPathResource("access.properties").getInputStream());

		String accessKey = properties.getProperty("accessKey");
		String secretKey = properties.getProperty("secretKey");

		AmazonRDSDataSourceFactoryBean factoryBean = new AmazonRDSDataSourceFactoryBean(accessKey, secretKey);

		factoryBean.setDatabaseName("test");
		CommonsDbcpDataSourceFactory dataSourceFactory = new CommonsDbcpDataSourceFactory();

		Properties dbProperties = new Properties();
		dbProperties.setProperty("logger","com.mysql.jdbc.log.Slf4JLogger");
		dbProperties.setProperty("profileSQL","true");
		
		dataSourceFactory.setConnectionProperties(dbProperties);
		
		factoryBean.setDataSourceFactory(dataSourceFactory);
		factoryBean.setDbInstanceIdentifier("test");
		factoryBean.setMasterUserName("test");
		factoryBean.setMasterUserPassword("secret");

		factoryBean.afterPropertiesSet();

		DataSource dataSource = factoryBean.getObject();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.query("SELECT 1", new RowMapper<Object>() {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return null;
			}
		});
		factoryBean.destroy();
	}

	@Test
	public void testCreateDataSource() throws Exception {
		Properties properties = new Properties();
		properties.load(new ClassPathResource("access.properties").getInputStream());

		String accessKey = properties.getProperty("accessKey");
		String secretKey = properties.getProperty("secretKey");

		AmazonRDSDataSourceFactoryBean factoryBean = new AmazonRDSDataSourceFactoryBean(accessKey, secretKey);

		factoryBean.setDatabaseName("test");
		factoryBean.setDataSourceFactory(new CommonsDbcpDataSourceFactory());
		factoryBean.setDbInstanceIdentifier("test2");
		factoryBean.setMasterUserName("master");
		factoryBean.setMasterUserPassword("myPass");

		factoryBean.setEngine("mysql");
		factoryBean.setEngineVersion("5.5.8");
		factoryBean.setAutoMinorVersionUpgrade(true);
		factoryBean.setMultiAz(false);
		factoryBean.setAvailabilityZone("us-east-1a");
		factoryBean.setPreferredBackupWindow("04:00-06:00");
		factoryBean.setPreferredMaintenanceWindow("Sun:23:15-Mon:03:15");
		factoryBean.setBackupRetentionPeriod(1);

		factoryBean.setAllocatedStorage(5);
		factoryBean.setAutoCreate(true);

		factoryBean.setPort(3306);
		factoryBean.setDbInstanceClass("db.m1.small");
		factoryBean.setDataSourceFactory(new CommonsDbcpDataSourceFactory());
		factoryBean.setDbSecurityGroups(Collections.singletonList("test"));

		factoryBean.afterPropertiesSet();

		DataSource dataSource = factoryBean.getObject();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.query("SELECT 1", new RowMapper<Object>() {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return null;
			}
		});

		factoryBean.destroy();
	}
}
