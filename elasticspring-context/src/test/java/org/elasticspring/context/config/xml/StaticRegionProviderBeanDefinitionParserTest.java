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

package org.elasticspring.context.config.xml;

import org.elasticspring.core.region.Region;
import org.elasticspring.core.region.RegionProvider;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 */
public class StaticRegionProviderBeanDefinitionParserTest {

	@Test
	public void testParseSimple() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		RegionProvider myRegionProvider = context.getBean("myRegionProvider", RegionProvider.class);
		Assert.assertNotNull(myRegionProvider);
		Assert.assertEquals(Region.SAO_PAULO, myRegionProvider.getRegion());
	}

	@Test
	public void testParseWithExpression() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-testWithExpression.xml", getClass());
		RegionProvider myRegionProvider = context.getBean("myRegionProvider", RegionProvider.class);
		Assert.assertNotNull(myRegionProvider);
		Assert.assertEquals(Region.SAO_PAULO, myRegionProvider.getRegion());
	}

	@Test
	public void testParseWithPlaceholder() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-testWithPlaceHolder.xml", getClass());
		RegionProvider myRegionProvider = context.getBean("myRegionProvider", RegionProvider.class);
		Assert.assertNotNull(myRegionProvider);
		Assert.assertEquals(Region.SAO_PAULO, myRegionProvider.getRegion());
	}
}