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
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.githubimporter.input.GithubImporterInputProperties;
import org.xwiki.contrib.githubimporter.internal.GithubImporterFileCatcher;
import org.xwiki.contrib.githubimporter.internal.GithubImporterFilter;
import org.xwiki.contrib.githubimporter.internal.GithubImporterSyntaxConverter;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.FileInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.URLInputSource;
import org.xwiki.git.GitManager;

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

    private static final String KEY_MARKDOWN_GITHUB = "markdown+github/1.0";

    private static final String KEY_MEDIAWIKI_SYNTAX = "mediawiki/1.6";

    private static final String KEY_CREOLE_SYNTAX = "creole/1.0";

    private static final String KEY_XWIKI_SYNTAX = "xwiki/2.1";

    private static final String KEY_URL_GIT = "\\.git";

    private static final String KEY_WEBHOME = "WebHome";

    private static final String KEY_ZIP = ".zip";

    private static final String KEY_FORWARD_SLASH = "/";

    private static final String KEY_GITHUB_IMPORTER_TEMPDIR = "GithubImporter";

    private static final String KEY_FILE_MD = ".md";

    private static final String KEY_FILE_MEDIAWIKI = ".mediawiki";

    private static final String KEY_FILE_CREOLE = ".creole";

    private static final String ERROR_EXCEPTION = "Error: An Exception was thrown.";

    @Inject
    private GitManager gitManager;

    @Inject
    private CoreConfiguration coreConfiguration;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Environment environment;

    @Inject
    private GithubImporterSyntaxConverter syntaxConverter;

    @Inject
    private GithubImporterFileCatcher fileCatcher;

    @Inject
    private Logger logger;

    @Override
    protected void read(Object filter, GithubImporterFilter filterHandler) throws FilterException
    {
        InputSource inputSource = this.properties.getSource();
        if (inputSource != null) {
            File wikiRepoDirectory = null;
            if (inputSource instanceof URLInputSource) {
                logger.warn("URL source!");
                String urlString = ((URLInputSource) inputSource).getURL().toString();
                Repository repo = gitManager.getRepository(urlString, getRepoName(urlString),
                    this.properties.getUsername(), this.properties.getAuthCode());
                wikiRepoDirectory = repo.getWorkTree();
            } else if (inputSource instanceof FileInputSource) {
                File file = ((FileInputSource) inputSource).getFile();
                // if the input source is a directory, set it as wikiRepoDirectory
                if (file.isDirectory()) {
                    wikiRepoDirectory = file;
                } else if (file.getName().endsWith(KEY_ZIP)) {
                    fileCatcher.extractZip(file.getAbsolutePath(), getTemporaryDirectoryPath());
                    String tempPath = getTemporaryDirectoryPath() + KEY_FORWARD_SLASH
                            + file.getName().split(KEY_ZIP)[0];
                    wikiRepoDirectory = new File(tempPath);
                }
            }
            if (wikiRepoDirectory != null) {
                if (this.properties.isCreateHierarchy()) {
                    readHierarchy(wikiRepoDirectory, filterHandler);
                } else {
                    readGitDirectory(wikiRepoDirectory, filterHandler);
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

    private void readGitDirectory(File directory, GithubImporterFilter filterHandler) throws FilterException
    {
        FileFilter fileFilter = file -> (!file.getName().startsWith(".") && !file.getName().startsWith("_"));
        File[] docArray = directory.listFiles(fileFilter);
        if (docArray != null) {
            Arrays.sort(docArray);
            String parentName = this.properties.getParent().toString().replaceAll("Space ", "");
            String[] spaces = parentName.split(KEY_DOT);
            for (int i = 0; i < spaces.length; i++) {
                filterHandler.beginWikiSpace(spaces[i], FilterEventParameters.EMPTY);
                if (i == spaces.length - 1) {
                    createParentContent(spaces[i], filterHandler);
                    readDirectoryRecursive(docArray, filterHandler);
                    for (int j = spaces.length - 1; j >= 0; j--) {
                        filterHandler.endWikiSpace(spaces[j], FilterEventParameters.EMPTY);
                    }
                }
            }
        }
    }

    private void readFile(File file, FilterEventParameters filterParams, String syntaxId,
                          GithubImporterFilter filterHandler) throws FilterException
    {
        try {
            String fileContents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            if (this.properties.isConvertSyntax()) {
                fileContents = syntaxConverter.getConvertedContent(fileContents, syntaxId);
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

    private String getRepoName(String urlString)
    {
        return urlString.substring(urlString.lastIndexOf(KEY_FORWARD_SLASH) + 1).split(KEY_URL_GIT)[0];
    }

    private FilterEventParameters getSyntaxParameters(String syntaxKey)
    {
        FilterEventParameters filterParams = new FilterEventParameters();
        if (!this.properties.isConvertSyntax()) {
            filterParams.put(WikiDocumentFilter.PARAMETER_SYNTAX, syntaxKey);
        }
        return filterParams;
    }

    private void readHierarchy(File directory, GithubImporterFilter filterHandler) throws FilterException
    {
        File sidebar = new File(directory, "_Sidebar.md");
        if (!sidebar.exists() || !sidebar.canRead()) {
            readGitDirectory(directory, filterHandler);
        } else {
            readSidebar(sidebar, directory, filterHandler);
        }
    }

    private void readSidebar(File sidebar, File directory, GithubImporterFilter filterHandler) throws FilterException
    {
        ArrayList<String> hierarchy = new ArrayList<>();
        hierarchy.add(this.properties.getParent().getName());
        filterHandler.beginWikiSpace(hierarchy.get(hierarchy.size() - 1),
                    FilterEventParameters.EMPTY);
        createParentContent(this.properties.getParent().getName(), filterHandler);
        final String[] parentName = {""};
        try (Stream<String> linesStream = Files.lines(sidebar.toPath())) {
            final AtomicInteger[] parentLevel = {new AtomicInteger()};
            linesStream.forEach(line -> {
                String pageDetailStart = line.substring(line.indexOf('['));
                String pageLink = pageDetailStart.substring(pageDetailStart.indexOf(']') + 2);
                String pageName = getPageName(pageLink);
                if (!pageName.equals("")) {
                    String pageFileName = pageName + KEY_FILE_MD;
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
                        readFileType(pageFile, filterHandler);
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

    private void readDirectoryRecursive(File[] docArray, GithubImporterFilter filterHandler) throws FilterException
    {
        if (docArray != null) {
            Arrays.sort(docArray);
            for (File file : docArray) {
                if (file.isDirectory()) {
                    filterHandler.beginWikiSpace(file.getName(), FilterEventParameters.EMPTY);
                    createParentContent(file.getName(), filterHandler);
                    readDirectoryRecursive(file.listFiles(), filterHandler);
                    filterHandler.endWikiSpace(file.getName(), FilterEventParameters.EMPTY);
                } else {
                    readFileType(file, filterHandler);
                }
            }
        }
    }

    private String getTemporaryDirectoryPath()
    {
        String githubImporterTempDir = "";
        try {
            githubImporterTempDir = environment.getTemporaryDirectory().getAbsolutePath()
                    + KEY_FORWARD_SLASH + KEY_GITHUB_IMPORTER_TEMPDIR;
        } catch (Exception ignored) {
        } finally {
            if (githubImporterTempDir.equals("")) {
                githubImporterTempDir = new File("target/tempdir").getAbsolutePath();
            }
        }
        return githubImporterTempDir;
    }

    private void createParentContent(String parentName, GithubImporterFilter filterHandler) throws FilterException
    {
        String parentContent = String.format("{{documents location=\"%s.\" columns=\"doc.title,"
                + "doc.location,doc.date\"}}", parentName);
        FilterEventParameters filterParams = new FilterEventParameters();
        filterParams.put(WikiDocumentFilter.PARAMETER_SYNTAX, KEY_XWIKI_SYNTAX);
        filterParams.put(WikiDocumentFilter.PARAMETER_CONTENT, parentContent);
        filterHandler.beginWikiDocument(KEY_WEBHOME, filterParams);
        filterHandler.endWikiDocument(KEY_WEBHOME, filterParams);
    }

    private void readFileType(File file, GithubImporterFilter filterHandler) throws FilterException
    {
        if (file.getName().endsWith(KEY_FILE_MD)) {
            readFile(file, getSyntaxParameters(KEY_MARKDOWN_GITHUB), KEY_MARKDOWN_GITHUB, filterHandler);
        }
        if (file.getName().endsWith(KEY_FILE_MEDIAWIKI)) {
            readFile(file, getSyntaxParameters(KEY_MEDIAWIKI_SYNTAX), KEY_MEDIAWIKI_SYNTAX,
                    filterHandler);
        }
        if (file.getName().endsWith(KEY_FILE_CREOLE)) {
            readFile(file, getSyntaxParameters(KEY_CREOLE_SYNTAX), KEY_CREOLE_SYNTAX, filterHandler);
        }
    }
}
