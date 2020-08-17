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

import org.xwiki.component.annotation.Role;
import org.xwiki.filter.FilterException;

/**
 * Provides methods for syntax conversion in GitHub Importer.
 *
 * @version $Id$
 * @since 1.3
 */
@Role
public interface GithubImporterSyntaxConverter
{
    /**
     * Converts content from Markdown to default syntax.
     *
     * @param content the source content to convert
     * @param syntaxId the id of the syntax to convert from
     * @return converted contents in default syntax
     * @since 1.3
     */
    String getConvertedContent(String content, String syntaxId) throws FilterException;
}
