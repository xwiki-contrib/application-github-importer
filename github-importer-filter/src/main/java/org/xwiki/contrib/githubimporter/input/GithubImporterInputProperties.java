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
package org.xwiki.contrib.githubimporter.input;

import org.xwiki.filter.DefaultFilterStreamProperties;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.type.SystemType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;

/**
 * GitHub Importer input properties.
 *
 * @version $Id$
 * @since 1.0
 */
public class GithubImporterInputProperties extends DefaultFilterStreamProperties
{
    /**
     * The GitHub Importer Wiki format as a single String.
     */
    public static final String FILTER_STREAM_TYPE_STRING = "githubimporter+wiki";

    /**
     * Create a new System Type for the GitHub Importer.
     */
    // @TODO: Remove when updated in the Filter Streams Extension
    private static final SystemType GITHUB_IMPORTER = new SystemType("githubimporter");

    /**
     * The Data Format for FilterStreamType.
     */
    private static final String DATA_WIKI = "wiki";

    /**
     * The Filter Stream Type for GitHub Wiki.
     */
    public static final FilterStreamType FILTER_STREAM_TYPE = new FilterStreamType(GITHUB_IMPORTER, DATA_WIKI);

    /**
     * @see #getSource()
     */
    private InputSource source;

    /**
     * @see #getParent()
     */
    private EntityReference parent;

    /**
     * @see #isConvertSyntax()
     */
    private boolean convertSyntax;

    /**
     * @see #getUsername()
     */
    private String username = "";

    /**
     * @see #getAuthCode()
     */
    private String authCode = "";

    /**
     * @return input source of GitHub Wiki
     */
    @PropertyName("Source")
    @PropertyDescription("The source to load the GitHub Wiki from.")
    @PropertyMandatory
    public InputSource getSource()
    {
        return this.source;
    }

    /**
     * @param source the new source for github wiki input
     */
    public void setSource(InputSource source)
    {
        this.source = source;
    }

    /**
     * @return the reference of the parent of all pages
     */
    @PropertyName("Parent")
    @PropertyDescription("The parent to all pages which are going to be imported.")
    public EntityReference getParent()
    {
        return this.parent;
    }

    /**
     * @param parent the reference of the parent of all pages
     */
    public void setParent(EntityReference parent)
    {
        // Since DOCUMENT is the default type in EntityReference parser so we have to convert it to space
        if (parent != null && parent.getType() == EntityType.DOCUMENT) {
            this.parent = new EntityReference(parent.getName(), EntityType.SPACE, parent.getParent());
        } else {
            this.parent = parent;
        }
    }

    /**
     * @return the syntax conversion choice
     * @since 1.1
     */
    @PropertyName("XWiki syntax conversion")
    @PropertyDescription("Convert syntax from Markdown to XWiki")
    public boolean isConvertSyntax()
    {
        return this.convertSyntax;
    }

    /**
     * @param convert the boolean to set conversion true or false
     * @since 1.1
     */
    public void setConvertSyntax(boolean convert)
    {
        this.convertSyntax = convert;
    }

    /**
     * @return the username to use for cloning protected repositories
     * @since 1.1
     */
    @PropertyName("(Optional) Username")
    @PropertyDescription("The username of GitHub.")
    public String getUsername()
    {
        return this.username;
    }

    /**
     * @param user the user to use for cloning repository
     * @since 1.1
     */
    public void setUsername(String user)
    {
        this.username = user;
    }

    /**
     * @return the authentication code used to clone protected repositories
     * @since 1.1
     */
    @PropertyName("(Optional) Authentication Code")
    @PropertyDescription("The oauth / token / password used to clone protected repositories.")
    public String getAuthCode()
    {
        return this.authCode;
    }

    /**
     * @param auth the auth or access code to use for cloning repository
     * @since 1.1
     */
    public void setAuthCode(String auth)
    {
        this.authCode = auth;
    }
}
