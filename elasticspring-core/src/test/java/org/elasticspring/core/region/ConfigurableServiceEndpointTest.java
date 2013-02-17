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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Agim Emruli
 */
public class ConfigurableServiceEndpointTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testGetRegion() throws Exception {
		ServiceEndpoint serviceEndpoint = new ConfigurableServiceEndpoint(Region.US_STANDARD, "test");
		Assert.assertEquals(Region.US_STANDARD, serviceEndpoint.getRegion());
	}

	@Test
	public void testGetLocation() throws Exception {
		ServiceEndpoint serviceEndpoint = new ConfigurableServiceEndpoint(Region.IRELAND, "test");
		Assert.assertEquals("eu-west-1", serviceEndpoint.getLocation());
	}

	@Test
	public void testGetWithCustomMapping() throws Exception {
		ServiceEndpoint serviceEndpoint = new ConfigurableServiceEndpoint(Region.IRELAND, "test", "eu-west-2");
		Assert.assertEquals("eu-west-2", serviceEndpoint.getLocation());
	}

	@Test
	public void testGetEndpoint() throws Exception {
		ServiceEndpoint serviceEndpoint = new ConfigurableServiceEndpoint(Region.IRELAND, "test");
		Assert.assertEquals("test.eu-west-1.amazonaws.com", serviceEndpoint.getEndpoint());
	}

	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	@Test
	public void testRegionNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("Region must not be null");
		new ConfigurableServiceEndpoint(null, "foo");
	}

	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	@Test
	public void testServiceNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("ServiceName must not be null");
		new ConfigurableServiceEndpoint(Region.IRELAND, null);
	}

	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	@Test
	public void testCustomLocationNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("Location must not be null");
		new ConfigurableServiceEndpoint(Region.IRELAND, "foo", null);
	}
}
