/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.core.region;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Agim Emruli
 */
public class Ec2MetadataRegionProviderTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getRegion_availabilityZoneWithMatchingRegion_returnsRegion() throws Exception {
		//Arrange
		Ec2MetadataRegionProvider regionProvider = new Ec2MetadataRegionProvider(){

			@Override
			protected String getAvailabilityZone() {
				return "eu-west-1a";
			}
		};

		//Act
		Region region = regionProvider.getRegion();

		//Assert
		Assert.assertEquals(Region.getRegion(Regions.EU_WEST_1),region);
	}

	@Test
	public void getRegion_availabilityZoneWithNonMatchingRegion_throwsIllegalStateException() throws Exception {
		//Arrange
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("There could be no region detected for the availability zone 'eu-east-1a'");

		Ec2MetadataRegionProvider regionProvider = new Ec2MetadataRegionProvider() {

			@Override
			protected String getAvailabilityZone() {
				return "eu-east-1a";
			}
		};

		//Act
		regionProvider.getRegion();

		//Assert
	}

	@Test
	public void getRegion_noMetadataAvailable_throwsIllegalStateException() throws Exception {
		//Arrange
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("There is not EC2 meta data available, because the application is not running in the EC2 environment");

		Ec2MetadataRegionProvider regionProvider = new Ec2MetadataRegionProvider(){

			@Override
			protected String getAvailabilityZone() {
				return null;
			}
		};

		//Act
		regionProvider.getRegion();

		//Assert
	}
}
