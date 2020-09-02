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
import java.util.HashMap;
import java.util.Map;
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
    private static final String ERROR_EXCEPTION = "Error: An Exception was thrown.";

    private static final String KEY_URL_GIT = ".git";

    private static final String KEY_FILE_MD = ".md";

    private static final String KEY_FORWARD_SLASH = "/";

    private static final String KEY_DOT = ".";

    private static final String KEY_DOT_REGEX = "\\.";

    private static final String KEY_MARKDOWN_GITHUB = "markdown+github/1.0";

    private static final String KEY_MEDIAWIKI_SYNTAX = "mediawiki/1.6";

    private static final String KEY_CREOLE_SYNTAX = "creole/1.0";

    private static final String KEY_XWIKI_SYNTAX = "xwiki/2.1";

    private static final String KEY_PLAIN_SYNTAX = "plain/1.0";

    private static final String KEY_WEBHOME = "WebHome";

    private static final String KEY_ZIP = ".zip";

    private static final String KEY_GITHUB_IMPORTER_TEMPDIR = "GithubImporter";

    private static final String KEY_FILE_MEDIAWIKI = ".mediawiki";

    private static final String KEY_FILE_CREOLE = ".creole";

    private static final String KEY_SPACE_STRING = "Space ";

    private static final String KEY_SPACE_WEBHOME = ".WebHome";

    private static final String KEY_GIT_URL_TREE = "/tree/";

    private static final String KEY_GIT_URL_BLOB = "/blob/";

    private static final String KEY_BULLET_DASH = "- ";

    private static final String KEY_BULLET_STERIC = "* ";

    private static final String KEY_SQUARE_BRACKET_START = "[";

    private static final String KEY_SQUARE_BRACKET_END = "]";

    private static final String KEY_HASH = "#";

    private static final String KEY_REGEX_TREE = "(/blob/)|(/tree/)";

    private static final String ERROR_SIDEBAR = "Sidebar is unreadable or unsupported. Please uncheck Create Hierarchy"
            + " or fix the Sidebar. ";

    private static final String ERROR_SIDEBAR_HEADING_START = "Failed creating space for heading. [{}]";

    private static final String ERROR_SIDEBAR_HEADING_END = "Failed ending space for heading. [{}]";

    private String parentName = "";

    private int parentLevel;

    private int spaceLevel;

    @Inject
    private Logger logger;

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

    @Override
    protected void read(Object filter, GithubImporterFilter filterHandler) throws FilterException
    {
        InputSource inputSource = this.properties.getSource();
        if (inputSource != null) {
            String parentReference = this.properties.getParent().toString().replaceAll(KEY_SPACE_STRING, "");
            logger.info("The pages will be created under the reference: [{}]", parentReference);
            File wikiRepoDirectory = null;
            if (inputSource instanceof URLInputSource) {
                String urlString = ((URLInputSource) inputSource).getURL().toString();
                logger.info("Cloning git repository from [{}]", urlString);
                Repository repo = gitManager.getRepository(urlString, getRepoName(urlString),
                    this.properties.getUsername(), this.properties.getAuthCode());
                wikiRepoDirectory = repo.getWorkTree();
            } else if (inputSource instanceof FileInputSource) {
                File file = ((FileInputSource) inputSource).getFile();
                // if the input source is a directory, set it as wikiRepoDirectory
                if (file.isDirectory()) {
                    wikiRepoDirectory = file;
                } else if (file.getName().endsWith(KEY_ZIP)) {
                    logger.info("Reading zip file from [{}]", file.getAbsolutePath());
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
            throw new FilterException("Input source is empty or not supported: [" + this.properties.getSource() + "] "
                + "Please specify a valid input source.");
        }
    }

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    private void readGitDirectory(File directory, GithubImporterFilter filterHandler) throws FilterException
    {
        FileFilter fileFilter = file -> (!file.getName().startsWith(KEY_DOT) && !file.getName().startsWith("_"));
        File[] docArray = directory.listFiles(fileFilter);
        if (docArray != null) {
            Arrays.sort(docArray);
            String parentReference = this.properties.getParent().toString().replaceAll(KEY_SPACE_STRING, "");
            if (parentReference.contains(KEY_SPACE_WEBHOME)) {
                parentReference = parentReference.replace(KEY_SPACE_WEBHOME, "");
            }
            String[] spaces = parentReference.split(KEY_DOT_REGEX);
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

    private void readFile(File file, String pageName, FilterEventParameters filterParams, String syntaxId,
                          GithubImporterFilter filterHandler) throws FilterException
    {
        try {
            String fileContents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            if (this.properties.isConvertSyntax()) {
                logger.info("Converting syntax from [{}] to default syntax.", syntaxId);
                fileContents = syntaxConverter.getConvertedContent(fileContents, syntaxId);
            }
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
            logger.info("Sidebar does not exist or is unreadable. Continuing without hierarchy.");
            readGitDirectory(directory, filterHandler);
        } else {
            logger.info("Sidebar is found. Reading sidebar to create hierarchy of pages.");
            readSidebar(sidebar, directory, filterHandler);
        }
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
                    String fileName = file.getName();
                    int dotIndex = fileName.lastIndexOf(KEY_DOT);
                    String pageName = dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
                    readFileType(file, pageName, filterHandler);
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

    private void readFileType(File file, String pageName, GithubImporterFilter filterHandler) throws FilterException
    {
        if (file.isDirectory()) {
            filterHandler.beginWikiSpace(file.getName(), FilterEventParameters.EMPTY);
            createParentContent(file.getName(), filterHandler);
            readDirectoryRecursive(file.listFiles(), filterHandler);
            filterHandler.endWikiSpace(file.getName(), FilterEventParameters.EMPTY);
        } else {
            if (file.getName().endsWith(KEY_FILE_MD)) {
                readFile(file, pageName, getSyntaxParameters(KEY_MARKDOWN_GITHUB), KEY_MARKDOWN_GITHUB, filterHandler);
            } else if (file.getName().endsWith(KEY_FILE_MEDIAWIKI)) {
                readFile(file, pageName, getSyntaxParameters(KEY_MEDIAWIKI_SYNTAX), KEY_MEDIAWIKI_SYNTAX,
                        filterHandler);
            } else if (file.getName().endsWith(KEY_FILE_CREOLE)) {
                readFile(file, pageName, getSyntaxParameters(KEY_CREOLE_SYNTAX), KEY_CREOLE_SYNTAX, filterHandler);
            } else if (!file.getName().contains(KEY_DOT)) {
                readFile(file, pageName, getSyntaxParameters(KEY_PLAIN_SYNTAX), KEY_PLAIN_SYNTAX, filterHandler);
            }
        }
    }

    private void readSidebar(File sidebar, File directory, GithubImporterFilter filterHandler) throws FilterException
    {
        ArrayList<String> hierarchy = new ArrayList<>();
        addParentToHierarchy(hierarchy, filterHandler);
        Map<String, String> additionalRepos = new HashMap<>();
        final boolean[] headingParent = {false};
        final String[] tempLineCatched = {""};
        try (Stream<String> linesStream = Files.lines(sidebar.toPath())) {
            linesStream.forEach(line -> {
                if (line.trim().startsWith(KEY_BULLET_DASH) || line.trim().startsWith(KEY_BULLET_STERIC)) {
                    readLevels(line, additionalRepos, directory, hierarchy, filterHandler);
                } else if (line.trim().startsWith(KEY_HASH)) {
                    headingParent[0] = readHeadingTypeLevel(line, headingParent[0], filterHandler, hierarchy);
                } else if (line.trim().startsWith("[[")) {
                    readDirectHeadingFile(line, directory, filterHandler);
                } else if (line.startsWith("---")) {
                    startUnderlinedHeadingSpace(hierarchy, filterHandler, tempLineCatched[0]);
                } else {
                    tempLineCatched[0] = line;
                }
            });
            if (hierarchy.size() > 0) {
                for (int j = hierarchy.size() - 1; j >= 0; j--) {
                    try {
                        filterHandler.endWikiSpace(hierarchy.get(hierarchy.size() - 1), FilterEventParameters.EMPTY);
                    } catch (FilterException e) {
                        logger.warn("Failed to end space for [{}] after reading hierarchy.",
                                hierarchy.get(hierarchy.size() - 1));
                    }
                    hierarchy.remove(hierarchy.size() - 1);
                }
            }
        } catch (Exception e) {
            throw new FilterException(ERROR_SIDEBAR + ERROR_EXCEPTION, e);
        }
    }

    private String getSidebarFileReference(String repoLink)
    {
        String fileReference = repoLink.split(KEY_REGEX_TREE)[1].split(KEY_FORWARD_SLASH)[1];
        if (fileReference.lastIndexOf(KEY_FORWARD_SLASH) >= 0) {
            fileReference = fileReference.substring(0, fileReference.lastIndexOf(KEY_FORWARD_SLASH));
        } else {
            fileReference = "";
        }
        return fileReference;
    }

    private String getPageName(String pageDetailStart)
    {
        return pageDetailStart.substring(1, pageDetailStart.indexOf(KEY_SQUARE_BRACKET_END));
    }

    private String getFileNameFromLink(String pageLink)
    {
        String pageName = "";
        if (pageLink.startsWith("http")) {
            pageName = pageLink.substring(pageLink.lastIndexOf('/') + 1);
            pageName = pageName.substring(0, pageName.indexOf(")"));
        }
        if (pageName.equals("wiki")) {
            pageName = "Home";
        }
        return pageName;
    }

    private void addParentToHierarchy(ArrayList<String> hierarchy, GithubImporterFilter filterHandler)
            throws FilterException
    {
        String parentReference = this.properties.getParent().toString().replaceAll(KEY_SPACE_STRING, "");
        if (parentReference.endsWith(KEY_SPACE_WEBHOME)) {
            parentReference = parentReference.replace(KEY_SPACE_WEBHOME, "");
        }
        String[] spaces = parentReference.split(KEY_DOT_REGEX);
        for (int i = 0; i < spaces.length; i++) {
            spaceLevel++;
            filterHandler.beginWikiSpace(spaces[i], FilterEventParameters.EMPTY);
            hierarchy.add(spaces[i]);
            if (i == spaces.length - 1) {
                createParentContent(spaces[i], filterHandler);
            }
        }
    }

    private void readLevels(String line, Map<String, String> additionalRepos, File directory,
                            ArrayList<String> hierarchy, GithubImporterFilter filterHandler)
    {
        int bulletLevel = !line.contains(KEY_BULLET_STERIC)
                ? line.indexOf(KEY_BULLET_DASH)
                : line.indexOf(KEY_BULLET_STERIC);
        if (line.contains(KEY_SQUARE_BRACKET_START) && line.contains("github.com")) {
            String pageDetailStart = line.substring(line.indexOf(KEY_SQUARE_BRACKET_START));
            String pageLink = pageDetailStart.substring(pageDetailStart.indexOf(KEY_SQUARE_BRACKET_END) + 2);
            String pageName = getPageName(pageDetailStart);
            String fileName = getFileNameFromLink(pageLink);
            if (pageName.equals("")) {
                return;
            }
            File pageFile = getFileFromHierarchy(line, fileName, pageLink, additionalRepos, directory);
            try {
                readBulletLevel(bulletLevel, pageName, pageFile, hierarchy, filterHandler);
            } catch (Exception e) {
                logger.warn("Bullet level could not be read correctly. Please fix it. [{}].", e.getMessage());
            }
            parentLevel = bulletLevel;
            parentName = pageName.endsWith(KEY_FILE_MD) ? pageName.split(KEY_FILE_MD)[0] : pageName;
        } else if (!line.contains(KEY_SQUARE_BRACKET_START)) {
            String parentPageName = line.substring(line.indexOf(" "));
            parentLevel = bulletLevel;
            parentName = parentPageName;
        }
    }

    private File getAdditionalRepoFile(String pageLink, Map<String, String> additionalRepos, String pageName)
    {
        String fileReference = getSidebarFileReference(pageLink);
        String repoLink = pageLink.split(KEY_REGEX_TREE)[0] + KEY_URL_GIT;
        if (!additionalRepos.containsKey(getRepoName(repoLink))) {
            logger.info("Cloning additional git repository as found in sidebar from [{}]", repoLink);
            Repository repo = gitManager.getRepository(repoLink, getRepoName(repoLink),
                    this.properties.getUsername(), this.properties.getAuthCode());
            additionalRepos.put(getRepoName(repoLink), repo.getWorkTree().getAbsolutePath());
        }
        return new File(additionalRepos.get(getRepoName(repoLink)) + fileReference, pageName);
    }

    private void readBulletLevel(int bulletLevel, String pageName, File pageFile, ArrayList<String> hierarchy,
                                 GithubImporterFilter filterHandler) throws FilterException
    {
        if (bulletLevel > parentLevel) {
            logger.info("Bullet list level has risen. Moving space level deeper into [{}].", parentName);
            hierarchy.add(parentName);
            filterHandler.beginWikiSpace(hierarchy.get(hierarchy.size() - 1), FilterEventParameters.EMPTY);
        } else if (bulletLevel < parentLevel) {
            logger.info("Bullet list level has fallen. Moving space level out of [{}].",
                    hierarchy.get(hierarchy.size() - 1));
            filterHandler.endWikiSpace(hierarchy.get(hierarchy.size() - 1), FilterEventParameters.EMPTY);
            hierarchy.remove(hierarchy.size() - 1);
        }
        readFileType(pageFile, pageName, filterHandler);
    }

    private File getFileFromHierarchy(String line, String pageName, String pageLink,
                                      Map<String, String> additionalRepos, File directory)
    {
        if ((line.contains(KEY_GIT_URL_BLOB) || line.contains(KEY_GIT_URL_TREE))
                && pageName.endsWith(KEY_FILE_MD)) {
            return getAdditionalRepoFile(pageLink, additionalRepos, pageName);
        } else {
            String pageFileName = pageName + KEY_FILE_MD;
            return new File(directory, pageFileName);
        }
    }

    private boolean readHeadingTypeLevel(String line, boolean headingParent, GithubImporterFilter filterHandler,
                                         ArrayList<String> hierarchy)
    {
        String parentPageName = line.substring(line.indexOf(" "));
        if (headingParent) {
            try {
                filterHandler.endWikiSpace(hierarchy.get(hierarchy.size() - 1),
                        FilterEventParameters.EMPTY);
            } catch (FilterException e) {
                logger.warn(ERROR_SIDEBAR_HEADING_END, e.getMessage());
            }
            hierarchy.remove(hierarchy.size() - 1);
        }
        hierarchy.add(parentPageName);
        try {
            filterHandler.beginWikiSpace(hierarchy.get(hierarchy.size() - 1), FilterEventParameters.EMPTY);
            createParentContent(hierarchy.get(hierarchy.size() - 1), filterHandler);
        } catch (FilterException e) {
            logger.warn(ERROR_SIDEBAR_HEADING_START, e.getMessage());
        }
        return true;
    }

    private void readDirectHeadingFile(String line, File directory, GithubImporterFilter filterHandler)
    {
        int nameIndex = line.lastIndexOf("|");
        nameIndex = nameIndex < 0 ? nameIndex + 3 : nameIndex + 1;
        String pageFileName = line.substring(nameIndex, line.lastIndexOf("]]"));
        if (pageFileName.contains(KEY_HASH)) {
            pageFileName = pageFileName.substring(0, pageFileName.lastIndexOf(KEY_HASH));
        }
        String fileNameMd = pageFileName + KEY_FILE_MD;
        try {
            File pageFile = new File(directory, fileNameMd);
            String pageName = pageFile.getName();
            readFileType(pageFile, pageName.substring(0, pageName.lastIndexOf(KEY_DOT)), filterHandler);
        } catch (Exception e) {
            try {
                File pageFile = new File(directory, fileNameMd.replace(" ", "-"));
                String pageName = pageFile.getName();
                readFileType(pageFile, pageName.substring(0, pageName.lastIndexOf(KEY_DOT)), filterHandler);
            } catch (Exception e2) {
                logger.warn("File not found named as [{}]. Skipping this file as it does not exist.", fileNameMd);
            }
        }
    }

    private void startUnderlinedHeadingSpace(ArrayList<String> hierarchy, GithubImporterFilter filterHandler,
                                             String tempLineCatched)
    {
        if (hierarchy.size() > spaceLevel) {
            try {
                filterHandler.endWikiSpace(hierarchy.get(hierarchy.size() - 1),
                        FilterEventParameters.EMPTY);
                hierarchy.remove(hierarchy.size() - 1);
            } catch (Exception e) {
                logger.warn(ERROR_SIDEBAR_HEADING_END, e.getMessage());
            }
        }
        try {
            hierarchy.add(tempLineCatched);
            filterHandler.beginWikiSpace(hierarchy.get(hierarchy.size() - 1), FilterEventParameters.EMPTY);
            createParentContent(hierarchy.get(hierarchy.size() - 1), filterHandler);
        } catch (Exception e) {
            logger.warn(ERROR_SIDEBAR_HEADING_START, e.getMessage());
        }
    }
}
