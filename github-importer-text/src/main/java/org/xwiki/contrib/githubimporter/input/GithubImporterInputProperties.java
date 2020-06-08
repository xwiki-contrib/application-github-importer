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
     * The GitHub Importer Text format.
     */
    // @TODO: Remove when updated in the Filter Streams Extension
    private static final SystemType GITHUBIMPORTER = new SystemType("githubimporter");

    /**
     * The Data Format for FilterStreamType.
     */
    private static final String DATA_TEXT = "text";

    /**
     * The GitHub Wiki format as String.
     */
    public static final String FILTER_STREAM_TYPE_STRING = "github+text";

    /**
     * The Filter Stream Type for GitHub Wiki.
     */
    public static final FilterStreamType FILTER_STREAM_TYPE = new FilterStreamType(GITHUBIMPORTER, DATA_TEXT);

    private InputSource source;

    private EntityReference parent;

    /**
     * @return input source of GitHub Wiki
     */
    @PropertyName("Source")
    @PropertyDescription("The source to load the github wiki from")
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
    @PropertyDescription("The reference of the parent of all pages")
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
}
