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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.githubimporter.internal.GithubImporterFileCatcher;
import org.xwiki.filter.FilterException;

import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provides methods for file operations in GitHub Importer.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class FileCatcher implements GithubImporterFileCatcher
{
    @Override
    public void extractZip(String source, String destination) throws FilterException
    {
        try {
            // Make buffer 2KB as default (max supported by BufferedInputStream read)
            int bufferSize = 2048;
            File file = new File(source);
            ZipFile zip = new ZipFile(file);
            Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
            // Get each zip entry
            while (zipFileEntries.hasMoreElements())
            {
                ZipEntry entry = zipFileEntries.nextElement();
                File destFile = new File(destination, entry.getName());
                File destinationParent = destFile.getParentFile();
                destinationParent.mkdirs();
                if (!entry.isDirectory())
                {
                    BufferedInputStream is = new BufferedInputStream(zip
                            .getInputStream(entry));
                    int currentByte;
                    // Create buffer
                    byte[] data = new byte[bufferSize];
                    FileOutputStream fos = new FileOutputStream(destFile);
                    BufferedOutputStream dest = new BufferedOutputStream(fos,
                            bufferSize);
                    // Write destination file
                    while ((currentByte = is.read(data, 0, bufferSize)) != -1) {
                        dest.write(data, 0, currentByte);
                    }
                    // Close
                    dest.flush();
                    dest.close();
                    is.close();
                }
            }
        } catch (Exception e) {
            throw new FilterException(String.format("Error extracting archive.\nSource: %s\nDestination: %s", source,
                    destination), e);
        }
    }
}
