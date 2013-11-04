package org.elasticspring.core.formation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("AmazonCloudFormationAwsTest-context.xml")
public class AmazonCloudFormationAwsTest {

	@Autowired(required = false)
	private AmazonStackResourceRegistry amazonStackResourceRegistry;

	@Test
	public void contextConfiguration_minimalContextConfiguration_amazonStackResourceRegistryBeanExposed() {
		// Assert
		assertThat(amazonStackResourceRegistry, is(not(nullValue())));
	}

}
