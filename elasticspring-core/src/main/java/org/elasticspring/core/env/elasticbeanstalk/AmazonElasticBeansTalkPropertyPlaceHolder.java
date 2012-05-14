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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.HashMap;
import java.util.Properties;

/**
 *
 */
public class AmazonElasticBeansTalkPropertyPlaceHolder extends PropertyPlaceholderConfigurer implements InitializingBean {

	private final AWSElasticBeanstalk awsElasticBeanstalk;
	private final HashMap<String, String> configurationSettings = new HashMap<String, String>();
	private String applicationName;

	public AmazonElasticBeansTalkPropertyPlaceHolder(String accessKey, String secretKey) {
		this.awsElasticBeanstalk = new AWSElasticBeanstalkClient(new BasicAWSCredentials(accessKey, secretKey));
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {
		String configurationOption = this.configurationSettings.get(placeholder);
		return configurationOption != null ? configurationOption : super.resolvePlaceholder(placeholder, props);
	}

	public void afterPropertiesSet() throws Exception {
		DescribeConfigurationSettingsResult describeConfigurationSettingsResult = this.awsElasticBeanstalk.describeConfigurationSettings(new DescribeConfigurationSettingsRequest(this.applicationName));
		for (ConfigurationSettingsDescription description : describeConfigurationSettingsResult.getConfigurationSettings()) {
			for (ConfigurationOptionSetting setting : description.getOptionSettings()) {
				this.configurationSettings.put(setting.getOptionName(), setting.getValue());
			}
		}

	}
}