package org.springframework.cloud.aws.jdbc.config.xml;

import java.sql.Connection;

import org.apache.tomcat.jdbc.pool.Validator;

/**
 * @author Agim Emruli
 */
public class SampleValidator implements Validator {

	@Override
	public boolean validate(Connection connection, int i) {
		return true;
	}

}
