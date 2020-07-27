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

import com.xpn.xwiki.CoreConfiguration;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterException;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

import javax.inject.Inject;
import java.io.StringReader;

/**
 * Provides various methods to assist Github Importer.
 *
 * @version $Id$
 * @since 1.3
 */
public class GithubImporterHelper
{
    @Inject
    private CoreConfiguration coreConfiguration;

    @Inject
    private ComponentManager componentManager;

    /**
     * Converts content from Markdown to default syntax.
     *
     * @param content the source content to convert
     * @return converted contents in default syntax
     * @since 1.3
     */
    public String getConvertedContent(String content) throws FilterException
    {
        String convertedContent;
        try {

            Syntax defaultSyntax = coreConfiguration.getDefaultDocumentSyntax();
            Converter converter = componentManager.getInstance(Converter.class);
            WikiPrinter printer = new DefaultWikiPrinter();
            converter.convert(new StringReader(content), new Syntax(SyntaxType.MARKDOWN, "1.2"), defaultSyntax,
                    printer);
            convertedContent = printer.toString();
        } catch (Exception e) {
            throw new FilterException("Error during syntax conversion.", e);
        }
        return convertedContent;
    }
}
