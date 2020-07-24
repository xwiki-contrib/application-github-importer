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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import com.xpn.xwiki.CoreConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.Repository;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.githubimporter.input.GithubImporterInputProperties;
import org.xwiki.contrib.githubimporter.internal.GithubImporterFilter;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.FileInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.URLInputSource;
import org.xwiki.git.GitManager;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * @version $Id$
 */
@Component
@Named(GithubImporterInputProperties.FILTER_STREAM_TYPE_STRING)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class GithubImporterInputFilterStream
    extends AbstractBeanInputFilterStream<GithubImporterInputProperties, GithubImporterFilter>
{
    private static final String KEY_DOT = "\\.";

    private static final String KEY_MARKDOWN = "markdown/1.2";

    private static final String KEY_URL_WIKI = ".wiki.git";

    private static final String KEY_URL_GIT = "\\.git";

    private static final String KEY_WEBHOME = "WebHome";

    private static final String ERROR_EXCEPTION = "Error: An Exception was thrown.";

    @Inject
    private GitManager gitManager;

    @Inject
    private CoreConfiguration coreConfiguration;

    @Inject
    private ComponentManager componentManager;

    @Override
    protected void read(Object filter, GithubImporterFilter filterHandler) throws FilterException
    {
        InputSource inputSource = this.properties.getSource();
        if (inputSource != null) {
            File wikiRepoDirectory = null;
            if (inputSource instanceof URLInputSource) {
                String urlString = ((URLInputSource) inputSource).getURL().toString();
                if (!urlString.endsWith(KEY_URL_WIKI) && urlString.endsWith(KEY_URL_GIT)) {
                    urlString = readWikiFromRepository(urlString);
                }
                Repository repo = gitManager.getRepository(urlString, getRepoName(urlString),
                    this.properties.getUsername(), this.properties.getAuthCode());
                wikiRepoDirectory = repo.getWorkTree();
            } else if (inputSource instanceof FileInputSource) {
                File file = ((FileInputSource) inputSource).getFile();
                if (file.isDirectory()) {
                    wikiRepoDirectory = file;
                }
            }
            if (wikiRepoDirectory != null) {
                if (this.properties.isCreateHierarchy()) {
                    readHierarchy(wikiRepoDirectory, filterHandler);
                } else {
                    readWikiDirectory(wikiRepoDirectory, filterHandler);
                }
            }
        } else {
            throw new FilterException("Input source is not supported: [" + this.properties.getSource() + "] "
                + "Please specify a valid Input source.");
        }
    }

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    private void readWikiDirectory(File directory, GithubImporterFilter filterHandler)
        throws FilterException
    {
        FileFilter fileFilter = file -> (FilenameUtils.getExtension(file.getName()).equals("md")
            && !file.getName().startsWith("_"));
        File[] docArray = directory.listFiles(fileFilter);
        if (docArray != null) {
            Arrays.sort(docArray);
            filterHandler.beginWikiSpace(this.properties.getParent().getName(), FilterEventParameters.EMPTY);
            for (File file : docArray) {
                readFile(file, getSyntaxParameters(filterHandler),
                        filterHandler);
            }
            filterHandler.endWikiSpace(this.properties.getParent().getName(), FilterEventParameters.EMPTY);
        }
    }

    private void readFile(File file, FilterEventParameters filterParams,
                          GithubImporterFilter filterHandler)
        throws FilterException
    {
        try {
            String fileContents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            if (this.properties.isConvertSyntax()) {
                fileContents = getConvertedContent(fileContents);
            }
            String pageName = file.getName().split(KEY_DOT)[0];
            filterParams.put(WikiDocumentFilter.PARAMETER_CONTENT, fileContents);
            filterHandler.beginWikiSpace(pageName, filterParams);
            filterHandler.beginWikiDocument(KEY_WEBHOME, filterParams);
            filterHandler.endWikiDocument(KEY_WEBHOME, filterParams);
            filterHandler.endWikiSpace(pageName, filterParams);
        } catch (Exception e) {
            throw new FilterException(ERROR_EXCEPTION, e);
        }
    }

    private String readWikiFromRepository(String urlString)
    {
        return urlString.split(KEY_URL_GIT)[0] + KEY_URL_WIKI;
    }

    private String getRepoName(String urlString)
    {
        return urlString.substring(urlString.lastIndexOf("/") + 1).split(KEY_URL_WIKI)[0];
    }

    private String getConvertedContent(String content) throws FilterException
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
            throw new FilterException(ERROR_EXCEPTION, e);
        }
        return convertedContent;
    }

    private FilterEventParameters getSyntaxParameters(GithubImporterFilter filterHandler)
    {
        FilterEventParameters filterParams = new FilterEventParameters();
        if (!this.properties.isConvertSyntax()) {
            filterParams.put(filterHandler.PARAMETER_SYNTAX, KEY_MARKDOWN);
        }
        return filterParams;
    }

    private void readHierarchy(File directory, GithubImporterFilter filterHandler) throws FilterException
    {
        File sidebar = new File(directory, "_Sidebar.md");
        if (!sidebar.exists() || !sidebar.canRead()) {
            throw new FilterException("Sidebar is unreadable or does not exist.");
        }
        readSidebar(sidebar, directory, filterHandler);
    }

    private void readSidebar(File sidebar, File directory, GithubImporterFilter filterHandler) throws FilterException
    {
        ArrayList<String> hierarchy = new ArrayList<>();
        hierarchy.add(this.properties.getParent().getName());
        try {
            filterHandler.beginWikiSpace(hierarchy.get(hierarchy.size() - 1),
                    FilterEventParameters.EMPTY);
        } catch (FilterException e) {
            e.printStackTrace();
        }
        final String[] parentName = {""};
        try (Stream<String> linesStream = Files.lines(sidebar.toPath())) {
            final AtomicInteger[] parentLevel = {new AtomicInteger()};
            linesStream.forEach(line -> {
                String pageDetailStart = line.substring(line.indexOf('['));
                String pageLink = pageDetailStart.substring(pageDetailStart.indexOf(']') + 2);
                String pageName = getPageName(pageLink);
                if (!pageName.equals("")) {
                    String pageFileName = pageName + ".md";
                    File pageFile = new File(directory, pageFileName);

                    int pageLevel = line.indexOf('*') < 0 ? line.indexOf('-') : line.indexOf('*');
                    try {
                        if (pageLevel == 0) {
                            parentName[0] = null;
                        } else if (pageLevel > parentLevel[0].get()) {
                            hierarchy.add(parentName[0]);
                            filterHandler.beginWikiSpace(hierarchy.get(hierarchy.size() - 1),
                                    FilterEventParameters.EMPTY);
                        } else if (pageLevel < parentLevel[0].get()) {
                            filterHandler.endWikiSpace(hierarchy.get(hierarchy.size() - 1),
                                    FilterEventParameters.EMPTY);
                            hierarchy.remove(hierarchy.size() - 1);
                        }
                        readFile(pageFile, getSyntaxParameters(filterHandler),
                                filterHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    parentLevel[0].set(pageLevel);
                    parentName[0] = pageName;
                }
            });
        } catch (Exception e) {
            throw new FilterException(ERROR_EXCEPTION, e);
        }
    }

    private String getPageName(String pageLink)
    {
        String pageName = "";
        if (pageLink.startsWith("http")) {
            pageName = pageLink.substring(pageLink.lastIndexOf('/') + 1);
            pageName = pageName.replace(")", "");
        }
        if (pageName.equals("wiki")) {
            pageName = "Home";
        }

        return pageName;
    }
}
