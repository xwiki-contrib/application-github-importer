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
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.githubimporter.internal.GithubImporterSyntaxConverter;
import org.xwiki.contrib.mediawiki.syntax.internal.parser.MediaWikiParser;
import org.xwiki.contrib.rendering.markdown.flavor.github.internal.parser.MarkdownGithubParser;
import org.xwiki.filter.FilterException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.internal.parser.creole.CreoleParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.StringReader;
import java.util.List;

/**
 * Provides methods for syntax conversion in GitHub Importer.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class SyntaxConverter implements GithubImporterSyntaxConverter
{
    private static final String KEY_FILE_HTML = ".html";

    private static final String KEY_FORWARD_SLASH = "/";

    /**
     * Required to get default document syntax of the user.
     */
    @Inject
    private CoreConfiguration coreConfiguration;

    /**
     * Required to get instance of converter for syntax conversion.
     */
    @Inject
    private ComponentManager componentManager;

    @Inject
    @Named("markdown+github/1.0")
    private Parser markdownGitHubParser;

    @Override
    public String getConvertedContent(String content, String syntaxId) throws FilterException
    {
        String convertedContent = content;
        try {
            Syntax defaultSyntax = coreConfiguration.getDefaultDocumentSyntax();
            Converter converter = componentManager.getInstance(Converter.class);
            WikiPrinter printer = new DefaultWikiPrinter();
            Syntax syntaxToConvert;
            switch (syntaxId) {
                case "markdown+github/1.0":
                    if (defaultSyntax.getType() == SyntaxType.XWIKI) {
                        convertedContent = fixedLinkContent(convertedContent, syntaxId);
                    }
                    syntaxToConvert = new MarkdownGithubParser().getSyntax();
                    break;
                case "mediawiki/1.6":
                    syntaxToConvert = new MediaWikiParser().getSyntax();
                    break;
                case "creole/1.0":
                    syntaxToConvert = new CreoleParser().getSyntax();
                    break;
                default:
                    return convertedContent;
            }
            converter.convert(new StringReader(convertedContent), syntaxToConvert, defaultSyntax,
                    printer);
            convertedContent = printer.toString();
        } catch (Exception e) {
            throw new FilterException("Error during syntax conversion.", e);
        }
        return convertedContent;
    }

    private String fixedLinkContent(String content, String syntaxId) throws ParseException, ComponentLookupException
    {
        String fixedContent;
        XDOM xdom = markdownGitHubParser.parse(new StringReader(content));
        List<LinkBlock> linkBlockList = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class),
                Block.Axes.DESCENDANT);
        for (LinkBlock linkBlock : linkBlockList) {
            String reference = linkBlock.getReference().getReference();
            if (reference.matches("^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*")) {
                if (reference.endsWith(KEY_FILE_HTML)) {
                    reference = reference.substring(0, reference.lastIndexOf(KEY_FILE_HTML));
                }
                if (reference.contains("/blob/") || reference.contains("/tree/")) {
                    reference = reference.split("(/blob/)|(/tree/)")[1];
                    reference = reference.substring(reference.indexOf(KEY_FORWARD_SLASH) + 1);
                }
                String localPageReference;
                if (reference.contains(KEY_FORWARD_SLASH)) {
                    localPageReference = "page:../";
                } else {
                    localPageReference = "page:./";
                }
                reference = localPageReference.concat(reference);
                LinkBlock newBlock = new LinkBlock(linkBlock.getChildren(), linkBlock.getReference(),
                        linkBlock.isFreeStandingURI());
                newBlock.getReference().setReference(reference);
                linkBlock.getParent().replaceChild(newBlock, linkBlock);
            }
        }
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        BlockRenderer blockRenderer = componentManager.getInstance(BlockRenderer.class, syntaxId);
        blockRenderer.render(xdom, wikiPrinter);
        fixedContent = wikiPrinter.toString();

        return fixedContent;
    }
}
