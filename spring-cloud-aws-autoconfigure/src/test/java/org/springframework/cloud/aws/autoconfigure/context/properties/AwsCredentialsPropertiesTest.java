/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.aws.autoconfigure.context.properties;

import java.util.UUID;

import com.amazonaws.auth.profile.internal.AwsProfileNameLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link AwsCredentialsProperties}.
 *
 * @author Tom Gianos
 * @since 2.0.2
 */
public class AwsCredentialsPropertiesTest {

	private AwsCredentialsProperties properties;

	@Before
	public void setup() {
		this.properties = new AwsCredentialsProperties();
	}

	@Test
	public void accessKeyCanBeSet() {
		Assert.assertNull("Access key default value expected to be null",
				this.properties.getAccessKey());

		String newAccessKey = UUID.randomUUID().toString();
		this.properties.setAccessKey(newAccessKey);
		Assert.assertEquals("Access key should have been assigned", newAccessKey,
				this.properties.getAccessKey());
	}

	@Test
	public void secretKeyCanBeSet() {
		Assert.assertNull("Secret key default value expected to be null",
				this.properties.getSecretKey());

		String newSecretKey = UUID.randomUUID().toString();
		this.properties.setSecretKey(newSecretKey);
		Assert.assertEquals("Secret key should have been assigned", newSecretKey,
				this.properties.getSecretKey());
	}

	@Test
	public void instanceProfileCanBeSet() {
		Assert.assertTrue("Instance profile default expected to be true",
				this.properties.isInstanceProfile());

		this.properties.setInstanceProfile(false);
		Assert.assertFalse("Instance profile should have been assigned",
				this.properties.isInstanceProfile());
	}

	@Test
	public void useDefaultAwsCredentialsChainCanBeSet() {
		Assert.assertFalse("useDefaultAwsCredentialsChain default expected to be false",
				this.properties.isUseDefaultAwsCredentialsChain());

		this.properties.setUseDefaultAwsCredentialsChain(true);
		Assert.assertTrue("useDefaultAwsCredentialsChain should have been assigned",
				this.properties.isUseDefaultAwsCredentialsChain());
	}

	@Test
	public void profileNameCanBeSet() {
		Assert.assertEquals("Default profile name expected to be set",
				AwsProfileNameLoader.DEFAULT_PROFILE_NAME,
				this.properties.getProfileName());

		String newProfileName = UUID.randomUUID().toString();
		this.properties.setProfileName(newProfileName);
		Assert.assertEquals("Profile name should have been assigned", newProfileName,
				this.properties.getProfileName());
	}

	@Test
	public void profilePathCanBeSet() {
		Assert.assertNull("Profile path default value expected to be null",
				this.properties.getProfilePath());

		String newProfilePath = UUID.randomUUID().toString();
		this.properties.setProfilePath(newProfilePath);
		Assert.assertEquals("Profile path should have been assigned", newProfilePath,
				this.properties.getProfilePath());
	}

}
