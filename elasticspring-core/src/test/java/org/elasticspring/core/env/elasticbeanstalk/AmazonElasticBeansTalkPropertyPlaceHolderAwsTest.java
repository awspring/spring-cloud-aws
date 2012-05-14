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

package org.elasticspring.core.env.elasticbeanstalk;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class AmazonElasticBeansTalkPropertyPlaceHolderAwsTest {

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testElasticBeans() throws Exception {

		PropertiesFactoryBean factoryBean = new PropertiesFactoryBean();
		factoryBean.setLocation(new ClassPathResource("access.properties"));
		factoryBean.afterPropertiesSet();
		Properties properties = factoryBean.getObject();

		String accessKey = properties.getProperty("accessKey");
		String secretKey = properties.getProperty("secretKey");

		AmazonElasticBeansTalkPropertyPlaceHolder amazonElasticBeansTalkPropertyPlaceHolder = new AmazonElasticBeansTalkPropertyPlaceHolder(accessKey, secretKey);
		amazonElasticBeansTalkPropertyPlaceHolder.resolvePlaceholder("test", new Properties());
	}


	@Test
	@IfProfileValue(name= "test-groups", value = "aws-test")
	public void testCreate() throws Exception {
		AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(new PropertiesCredentials(new ClassPathResource("access.properties").getInputStream()));
		CheckDNSAvailabilityResult checkDNSAvailabilityResult = awsElasticBeanstalk.checkDNSAvailability(new CheckDNSAvailabilityRequest("greenhouse"));

		ListAvailableSolutionStacksResult listAvailableSolutionStacksResult = awsElasticBeanstalk.listAvailableSolutionStacks();
//		for (String solutionStack : listAvailableSolutionStacksResult.getSolutionStacks()) {
//			System.out.println("solutionStack = " + solutionStack);
//		}


//		DescribeApplicationsResult describeApplicationsResult = awsElasticBeanstalk.describeApplications(new DescribeApplicationsRequest().withApplicationNames("greenhouse"));
//		ApplicationDescription applicationDescription = describeApplicationsResult.getApplications().get(0);

//		System.out.println("applicationDescription = " + applicationDescription);

		DescribeConfigurationOptionsResult configurationOptionsResult = awsElasticBeanstalk.describeConfigurationOptions(new DescribeConfigurationOptionsRequest().withApplicationName("greenhouse").withTemplateName("Default"));


//		configurationOptionsResult = awsElasticBeanstalk.describeConfigurationOptions(new DescribeConfigurationOptionsRequest().withSolutionStackName("32bit Amazon Linux running Tomcat 6"));
//		for (ConfigurationOptionDescription description : configurationOptionsResult.getOptions()) {
//			if (!description.isUserDefined()) {
//				System.out.println("description = " + description);
//			}
//		}

//		CreateConfigurationTemplateRequest createConfigurationTemplateRequest = new CreateConfigurationTemplateRequest("greenhouse","greenhouse").withOptionSettings(new ConfigurationOptionSetting[]{new ConfigurationOptionSetting("aws:elasticbeanstalk:application:environment", "PARAM1", "test")});
//		CreateConfigurationTemplateResult configurationTemplate = awsElasticBeanstalk.createConfigurationTemplate(createConfigurationTemplateRequest);

		DescribeConfigurationSettingsResult describeConfigurationSettingsResult = awsElasticBeanstalk.describeConfigurationSettings(new DescribeConfigurationSettingsRequest().withApplicationName("greenhouse").withTemplateName("greenhouse"));
		for (ConfigurationSettingsDescription description : describeConfigurationSettingsResult.getConfigurationSettings()) {
			System.out.println("description = " + description);
			Collections.sort(description.getOptionSettings(),new Comparator<ConfigurationOptionSetting>() {

				public int compare(ConfigurationOptionSetting o, ConfigurationOptionSetting o1) {
					return o.getNamespace().compareTo(o1.getNamespace());
				}
			});

			for (ConfigurationOptionSetting setting : description.getOptionSettings()) {
//				if(setting.getOptionName().equals("PARAM1")){
					System.out.println("setting = " + setting);
//				}
			}
		}

//		CreateApplicationVersionResult applicationVersion = awsElasticBeanstalk.createApplicationVersion(new CreateApplicationVersionRequest("greenhouse", "1.0"));
//		System.out.println("applicationVersion = " + applicationVersion);


//		CreateEnvironmentResult environment = awsElasticBeanstalk.createEnvironment(new CreateEnvironmentRequest().withEnvironmentName("test").withApplicationName("greenhouse").withCNAMEPrefix("greenhouse").withDescription("greenhouse environment").withTemplateName("greenhouse"));
//		System.out.println("environment = " + environment);

		DescribeEnvironmentsResult describeEnvironmentsResult = awsElasticBeanstalk.describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentIds("e-id7zujharg"));
		System.out.println("describeEnvironmentsResult = " + describeEnvironmentsResult);

		DescribeEnvironmentResourcesResult describeEnvironmentResourcesResult = awsElasticBeanstalk.describeEnvironmentResources(new DescribeEnvironmentResourcesRequest().withEnvironmentId("e-id7zujharg"));
		System.out.println("describeEnvironmentResourcesResult = " + describeEnvironmentResourcesResult);

		AmazonEC2 amazonEC2 = new AmazonEC2Client(new PropertiesCredentials(new ClassPathResource("access.properties").getInputStream()));
		DescribeInstancesResult instancesResult = amazonEC2.describeInstances(new DescribeInstancesRequest().withInstanceIds("i-94e3f3fb"));
		for (Reservation reservation : instancesResult.getReservations()) {
			System.out.println("reservation = " + reservation);
			for (Instance instance : reservation.getInstances()) {

			}
		}
	}
}
