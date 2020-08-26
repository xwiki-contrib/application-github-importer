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
package org.xwiki.contrib.githubimporter.test.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testcontainers.shaded.com.google.common.io.Resources;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Functional tests to prove the working of GitHub Importer Filter.
 *
 * @version $Id$
 * @since 1.2
 */
@UITest
public class GithubImporterIT
{
    @Test
    @Order(1)
    public void assertInApplicationsPanel(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // Navigate to the GitHub Importer by clicking in the Application Panel.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("GitHub Importer");

        // Verify we're on the right page!
        Assert.assertEquals("GitHub Importer", vp.getMetaDataValue("space"));
        Assert.assertEquals("WebHome", vp.getMetaDataValue("page"));
    }

    @Test
    @Order(2)
    public void importGithubWiki(TestUtils setup, XWikiWebDriver driver)
        throws IOException, InterruptedException
    {
//        setup.loginAsSuperAdmin();

        // Go to Filter Application page
//        ApplicationIndexHomePage applicationIndexHomePage = ApplicationIndexHomePage.gotoPage();
//        Assert.assertTrue(applicationIndexHomePage.containsApplication("GitHub Importer"));
//        ViewPage vp = applicationIndexHomePage.clickApplication("GitHub Importer");
//        ViewPage vp = applicationIndexHomePage.clickApplication("GitHub Importer");

//        ViewPage vp = setup.gotoPage("GitHub Importer","WebHome");

        // Set input
//        Select inputType = new Select(driver.findElement(By.id("filter_input_type")));
//        inputType.selectByValue("githubimporter+wiki");

        WebElement inputElement = driver.findElement(By.id("githubimporter_properties_descriptor_source_input"));
        String url = "https://github.com/Haxsen/TestRepo.wiki.git";
        inputElement.sendKeys(url);

        inputElement = driver.findElement(By.id("githubimporter_properties_descriptor_parent"));
        inputElement.sendKeys("GithubImporterTestParent");

        // Set output
//        Select outputType = new Select(driver.findElement(By.id("filter_output_type")));
//        outputType.selectByValue("xwiki+instance");

        // Start conversion
        WebElement submit = driver.findElement(By.name("import"));
        submit.click();

        // Wait for conversion (15 seconds)
        // @TODO: Use better implementation when available on Filter Module
        Thread.sleep(15*1000);

        // Check the output
//        ViewPage viewPage = setup.gotoPage("GithubImporterTestParent","WebHome");
//        viewPage = setup.gotoPage("Home","WebHome");
        EntityReference importedReference = new DocumentReference("xwiki",
            Arrays.asList("GithubImporterTestParent", "Home"), "WebHome");
        ViewPage viewPage = setup.gotoPage(importedReference);
        String pageContent = viewPage.getContent();
        URL resourceURL = getClass().getResource("/TestWikiRepository/Home.resource");
        String resourceContent = Resources.toString(resourceURL, StandardCharsets.UTF_8);
//        String content = IOUtils.resourceToString(resourceURL.getFile(), Charset.defaultCharset());
//        FileUtils.readFileToString();
        Assert.assertEquals(resourceContent, pageContent);
    }
}
