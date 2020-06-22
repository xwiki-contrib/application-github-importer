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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.Repository;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.githubimporter.input.GithubImporterInputProperties;
import org.xwiki.contrib.githubimporter.internal.GithubImporterFilter;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.URLInputSource;
import org.xwiki.git.GitManager;
import org.xwiki.model.reference.EntityReference;

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

//    private static final String KEY_GIT_DIRECTORY = "GitHubImporterApplication";

    private static final String KEY_MARKDOWN = "markdown/1.2";

    private static final String KEY_URL_WIKI = ".wiki.git";

    private static final String KEY_URL_GIT = "\\.git";

    @Inject
    private GitManager gitManager;

    @Override
    protected void read(Object filter, GithubImporterFilter filterHandler) throws FilterException
    {
        InputSource inputSource = this.properties.getSource();
        if (inputSource != null) {
            File wikiRepoDirectory = null;
            if (inputSource instanceof URLInputSource) {
                String urlString = ((URLInputSource) inputSource).getURL().toString();
                if (!urlString.endsWith(KEY_URL_WIKI)) {
                    urlString = readWikiFromRepository(urlString);
                }
                Repository repo = gitManager.getRepository(urlString, getRepoName(urlString));
                wikiRepoDirectory = repo.getWorkTree();
            }
            if (wikiRepoDirectory != null) {
                readWikiDirectory(wikiRepoDirectory, filterHandler);
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
            FilterEventParameters filterParams = new FilterEventParameters();
            filterParams.put(filterHandler.PARAMETER_SYNTAX, KEY_MARKDOWN);
            for (File doc : docArray) {
                readFile(doc, filterParams, filterHandler);
            }
        }
    }

    private void readFile(File file, FilterEventParameters filterParams, GithubImporterFilter filterHandler)
        throws FilterException
    {
        try {
            String fileContents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            filterParams.put(WikiDocumentFilter.PARAMETER_CONTENT, fileContents);
            EntityReference reference = this.properties.getParent();
            filterHandler.beginWikiSpace(reference.getName(), FilterEventParameters.EMPTY);
            String pageName = file.getName().split(KEY_DOT)[0];
            filterHandler.beginWikiDocument(pageName, filterParams);
            filterHandler.endWikiDocument(pageName, filterParams);
            filterHandler.endWikiSpace(reference.getName(), FilterEventParameters.EMPTY);
        } catch (Exception e) {
            throw new FilterException("Error: An Exception was thrown.", e);
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
}
