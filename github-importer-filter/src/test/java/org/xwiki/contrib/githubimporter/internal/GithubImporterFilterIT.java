/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.githubimporter.internal;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.xwiki.contrib.githubimporter.internal.input.GithubImporterInputFilterStream;
import org.xwiki.environment.Environment;
import org.xwiki.filter.test.integration.FilterTestSuite;
import org.xwiki.filter.test.integration.FilterTestSuite.Scope;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.File;

import static org.mockito.Mockito.when;

/**
 * Run all tests found in the classpath. These {@code *.test} files must follow the conventions described in {@link
 * org.xwiki.filter.test.integration.TestDataParser}.
 *
 * @version $Id$
 */
@RunWith(FilterTestSuite.class)
@AllComponents
@Scope(value = "githubimporter")
public class GithubImporterFilterIT extends TestCase
{
    @Rule
    public MockitoComponentMockingRule<GithubImporterInputFilterStream> mocker =
            new MockitoComponentMockingRule<>(GithubImporterInputFilterStream.class);

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        Environment environment = this.mocker.registerMockComponent(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(getTemporaryDirectory());
    }

    private static File getTemporaryDirectory()
    {
        return new File("target/tempdir");
    }
}
