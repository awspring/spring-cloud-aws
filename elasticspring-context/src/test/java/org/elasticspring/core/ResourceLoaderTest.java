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

package org.elasticspring.core;

import org.elasticspring.core.s3.SimpleStorageResourceLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ResourceLoaderTest {


	@Autowired
	private ResourceLoaderClientBean clientBean;

	@Test
	public void testInjectedResourceLoader() throws Exception {
		assertTrue(clientBean.getResourceLoaderAwareResourceLoader() instanceof SimpleStorageResourceLoader);
		assertTrue(clientBean.getAnnotationResourceLoader() instanceof SimpleStorageResourceLoader);

		Resource resource = clientBean.getApplicationContext().getResource("s3://org.elasticspring/test.xml");
		assertTrue(resource.exists());
		InputStreamReader inputStreamReader = new InputStreamReader(resource.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		System.out.println("bufferedReader = " + bufferedReader.readLine());

	}
}
