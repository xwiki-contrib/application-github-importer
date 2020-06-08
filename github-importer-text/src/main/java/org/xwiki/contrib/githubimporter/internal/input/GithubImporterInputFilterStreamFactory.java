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
package org.xwiki.contrib.githubimporter.internal.input;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.githubimporter.input.GithubImporterInputProperties;
import org.xwiki.contrib.githubimporter.internal.GithubImporterFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStreamFactory;

/**
 * Create DokuWiki XML format input filters.
 *
 * @version $Id$
 */
@Component
@Named(GithubImporterInputProperties.FILTER_STREAM_TYPE_STRING)
@Singleton
public class GithubImporterInputFilterStreamFactory
    extends AbstractBeanInputFilterStreamFactory<GithubImporterInputProperties, GithubImporterFilter>
{
    /**
     * The default constructor.
     */
    public GithubImporterInputFilterStreamFactory()
    {
        super(GithubImporterInputProperties.FILTER_STREAM_TYPE);

        setName("GitHub Importer input stream");
        setDescription("Generates wiki events from Github Importer package.");
    }
}
