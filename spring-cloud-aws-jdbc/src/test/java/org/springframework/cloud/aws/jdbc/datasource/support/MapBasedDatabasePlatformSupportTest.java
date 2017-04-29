/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.jdbc.datasource.support;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit test class for {@link org.springframework.cloud.aws.jdbc.datasource.support.MapBasedDatabasePlatformSupport}
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class MapBasedDatabasePlatformSupportTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testGetDriverClassNameForDatabase() throws Exception {
        assertEquals("com.mysql.jdbc.Driver", new SimpleDatabasePlatformSupport().getDriverClassNameForDatabase(DatabaseType.MYSQL));
    }

    @Test
    public void testGetDriverNonConfiguredDatabasePlatform() throws Exception {
        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("No driver");
        new SimpleDatabasePlatformSupport().getDriverClassNameForDatabase(DatabaseType.ORACLE);
    }

    @Test
    public void testNullDriver() throws Exception {
        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("must not be null");
        new SimpleDatabasePlatformSupport().getDriverClassNameForDatabase(null);
    }

    @Test
    public void testGetDatabaseUrlForDatabase() throws Exception {
        SimpleDatabasePlatformSupport simpleDatabasePlatformSupport = new SimpleDatabasePlatformSupport();
        String url = simpleDatabasePlatformSupport.getDatabaseUrlForDatabase(DatabaseType.MYSQL, "localhost", 3306, "testDb");
        assertEquals("jdbc:mysql://localhost:3306/testDb", url);
    }

    @Test
    public void testGetDatabaseUrlWrongHostName() throws Exception {
        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("Error constructing URI from Host:'localhost<'");
        SimpleDatabasePlatformSupport simpleDatabasePlatformSupport = new SimpleDatabasePlatformSupport();
        String url = simpleDatabasePlatformSupport.getDatabaseUrlForDatabase(DatabaseType.MYSQL, "localhost<", 3306, "testDb");
        assertEquals("jdbc:mysql://localhost:3306/testDb", url);
    }


    private static class SimpleDatabasePlatformSupport extends MapBasedDatabasePlatformSupport {

        @Override
        protected Map<DatabaseType, String> getDriverClassNameMappings() {
            return Collections.singletonMap(DatabaseType.MYSQL, "com.mysql.jdbc.Driver");
        }

        @Override
        protected Map<DatabaseType, String> getSchemeNames() {
            return Collections.singletonMap(DatabaseType.MYSQL, "jdbc:mysql");
        }

        @Override
        protected Map<DatabaseType, String> getAuthenticationInfo() {
            return Collections.emptyMap();
        }
    }
}
