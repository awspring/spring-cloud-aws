package org.springframework.cloud.aws.jdbc.config.xml;

import org.apache.tomcat.jdbc.pool.Validator;

import java.sql.Connection;

/**
 * @author Agim Emruli
 */
public class SampleValidator implements Validator {

	@Override
	public boolean validate(Connection connection, int i) {
		return true;
	}
}
