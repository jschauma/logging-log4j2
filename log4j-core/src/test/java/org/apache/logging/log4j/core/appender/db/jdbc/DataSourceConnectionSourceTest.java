/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.db.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockejb.jndi.MockContextFactory;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DataSourceConnectionSourceTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        {"java:/comp/env/jdbc/Logging01"},
                        {"java:/comp/env/jdbc/Logging02"}
                }
        );
    }

    private final String jndiURL;
    private InitialContext context;

    public DataSourceConnectionSourceTest(final String jndiURL) {
        this.jndiURL = jndiURL;
    }

    @Before
    public void setUp() throws NamingException {
        MockContextFactory.setAsInitial();

        this.context = new InitialContext();
        this.context.createSubcontext("java:");
        this.context.createSubcontext("java:/comp");
        this.context.createSubcontext("java:/comp/env");
        this.context.createSubcontext("java:/comp/env/jdbc");
    }

    @After
    public void tearDown() {
        MockContextFactory.revertSetAsInitial();
    }

    @Test
    public void testNoJndiName01() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource(null);

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testNoJndiName02() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource("");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testNoDataSource() {
        final DataSourceConnectionSource source = DataSourceConnectionSource
                .createConnectionSource(this.jndiURL);

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testDataSource() throws NamingException, SQLException {
        final DataSource dataSource = createStrictMock(DataSource.class);
        final Connection connection1 = createStrictMock(Connection.class);
        final Connection connection2 = createStrictMock(Connection.class);

        expect(dataSource.getConnection()).andReturn(connection1);
        expect(dataSource.getConnection()).andReturn(connection2);
        replay(dataSource, connection1, connection2);

        this.context.bind(this.jndiURL, dataSource);

        DataSourceConnectionSource source = DataSourceConnectionSource
                .createConnectionSource(this.jndiURL);

        assertNotNull("The connection source should not be null.", source);
        assertEquals("The toString value is not correct.", "dataSource{ name=" + jndiURL + ", value="
                + dataSource + " }", source.toString());
        assertSame("The connection is not correct (1).", connection1, source.getConnection());
        assertSame("The connection is not correct (2).", connection2, source.getConnection());

        source = DataSourceConnectionSource.createConnectionSource(jndiURL.substring(0, jndiURL.length() - 1));

        assertNull("The connection source should be null now.", source);

        verify(dataSource, connection1, connection2);
    }

}
