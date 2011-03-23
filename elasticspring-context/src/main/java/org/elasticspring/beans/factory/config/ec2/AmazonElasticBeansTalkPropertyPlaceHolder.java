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

package org.elasticspring.beans.factory.config.ec2;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.List;
import java.util.Properties;

/**
 *
 */
public class AmazonElasticBeansTalkPropertyPlaceHolder extends PropertyPlaceholderConfigurer {

	private final AWSElasticBeanstalk awsElasticBeanstalk;

	public AmazonElasticBeansTalkPropertyPlaceHolder(String accessKey, String secretKey) {
		this.awsElasticBeanstalk = new AWSElasticBeanstalkClient(new BasicAWSCredentials(accessKey, secretKey));
	}

	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {
		DescribeApplicationsResult result = awsElasticBeanstalk.describeApplications();
		for (ApplicationDescription applicationDescription : result.getApplications()) {
			System.out.println("applicationDescription = " + applicationDescription);
			DescribeConfigurationSettingsResult describeConfigurationSettingsResult = this.awsElasticBeanstalk.describeConfigurationSettings(new DescribeConfigurationSettingsRequest(applicationDescription.getApplicationName()).withTemplateName(applicationDescription.getConfigurationTemplates().get(0)));
			List<ConfigurationSettingsDescription> configurationSettings = describeConfigurationSettingsResult.getConfigurationSettings();
			for (ConfigurationSettingsDescription configurationSetting : configurationSettings) {
				for (ConfigurationOptionSetting description : configurationSetting.getOptionSettings()) {
					System.out.println("description = " + description);
				}
			}

		}

		return super.resolvePlaceholder(placeholder, props);	//To change body of overridden methods use File | Settings | File Templates.
	}
}
