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

package org.elasticspring.jdbc.rds;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSecurityGroup;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;

/**
 *
 */
public class AmazonRDSDataSourceFactoryBean extends AbstractFactoryBean<DataSource> {

	private final AmazonRDS amazonRDS;

	public AmazonRDSDataSourceFactoryBean(String accessKey, String secretKey) {
		this.amazonRDS = new AmazonRDSClient(new BasicAWSCredentials(accessKey, secretKey));
	}


	@Override
	public Class<DataSource> getObjectType() {
		return DataSource.class;
	}

	@Override
	protected DataSource createInstance() throws Exception {
		DescribeDBInstancesResult dbInstancesResult = this.amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("instance1"));

		DBInstance instance;
		if (dbInstancesResult.getDBInstances().isEmpty()) {
			CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest("instance1", 5, "db.m1.small", "mysql", "user", "secret").withDBName("test");
			instance = this.amazonRDS.createDBInstance(createDBInstanceRequest);
		}else{
			instance = dbInstancesResult.getDBInstances().get(0);
		}

		DescribeDBSecurityGroupsResult securityGroupsResult = this.amazonRDS.describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest().withDBSecurityGroupName("test"));
		DBSecurityGroup securityGroup;
		if(securityGroupsResult.getDBSecurityGroups().isEmpty()){
			securityGroup = this.amazonRDS.createDBSecurityGroup(new CreateDBSecurityGroupRequest("test", "Test security group"));
		}else{
			securityGroup = securityGroupsResult.getDBSecurityGroups().get(0);
		}


//		DBSecurityGroup dbSecurityGroup = this.amazonRDS.authorizeDBSecurityGroupIngress(new AuthorizeDBSecurityGroupIngressRequest(securityGroup.getDBSecurityGroupName()).withCIDRIP("0.0.0.0/0"));
		instance = this.amazonRDS.modifyDBInstance(new ModifyDBInstanceRequest("instance1").withDBSecurityGroups("test"));
//		System.out.println("dbSecurityGroup = " + dbSecurityGroup);


		String connectionUrl = "jdbc:mysql://" + instance.getEndpoint().getAddress() + ":" + instance.getEndpoint().getPort() + "/test";

		return new SingleConnectionDataSource(connectionUrl, "user", "secret", true);
	}
}
