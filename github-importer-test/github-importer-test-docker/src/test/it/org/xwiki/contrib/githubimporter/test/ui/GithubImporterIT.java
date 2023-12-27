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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests to prove the working of GitHub Importer Filter.
 *
 * @version $Id$
 * @since 1.2
 */
@UITest
class GithubImporterIT
{
    @Test
    @Order(1)
    void assertInApplicationsPanel(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // Navigate to the GitHub Importer by clicking in the Application Panel.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("GitHub Importer");

        // Verify we're on the right page!
        assertEquals("GitHub Importer", vp.getMetaDataValue("space"));
        assertEquals("WebHome", vp.getMetaDataValue("page"));
    }

    @Test
    @Order(2)
    void importGithubWiki(TestUtils setup, XWikiWebDriver driver) throws IOException, InterruptedException
    {
        // setup.loginAsSuperAdmin();

        // Go to Filter Application page
        // ApplicationIndexHomePage applicationIndexHomePage = ApplicationIndexHomePage.gotoPage();
        // Assert.assertTrue(applicationIndexHomePage.containsApplication("GitHub Importer"));
        // ViewPage vp = applicationIndexHomePage.clickApplication("GitHub Importer");
        // ViewPage vp = applicationIndexHomePage.clickApplication("GitHub Importer");

        // ViewPage vp = setup.gotoPage("GitHub Importer","WebHome");

        // Set input
        // Select inputType = new Select(driver.findElement(By.id("filter_input_type")));
        // inputType.selectByValue("githubimporter+wiki");

        // Select type to File / Directory
        WebElement inputType = driver.findElement(By.name("githubimporter_sourcetype"));
        inputType.click();
        inputType = driver.findElement(By.name("githubimporter_sourcetype_file"));
        inputType.click();

        // Get resource url to use it as path for input source
        URL resourceURL = getClass().getResource("/TestRepository");
        WebElement inputElement = driver.findElement(By.id("githubimporter_properties_descriptor_source_file_input"));
        inputElement.sendKeys(resourceURL.getPath());

        // WebElement inputElement = driver.findElement(By.id("githubimporter_properties_descriptor_source_input"));
        // String url = "https://github.com/Haxsen/TestRepo.wiki.git";
        // inputElement.sendKeys(url);

        inputElement = driver.findElement(By.id("githubimporter_properties_descriptor_parent"));
        inputElement.sendKeys("GithubImporterTestParent");

        // Set output
        // Select outputType = new Select(driver.findElement(By.id("filter_output_type")));
        // outputType.selectByValue("xwiki+instance");

        // Start conversion
        WebElement submit = driver.findElement(By.name("import"));
        submit.click();

        // Wait for conversion (9 seconds)
        // convertJob.join() is a possible alternative
        // @TODO: Use better implementation when available on Filter Module
        Thread.sleep(9 * 1000);

        // Check the output
        // ViewPage viewPage = setup.gotoPage("GithubImporterTestParent","WebHome");
        // viewPage = setup.gotoPage("Home","WebHome");
        // String content = IOUtils.resourceToString(resourceURL.getFile(), Charset.defaultCharset());
        // FileUtils.readFileToString();

        // Go to the imported pages and assert their content
        EntityReference importedReference =
            new DocumentReference("xwiki", Arrays.asList("GithubImporterTestParent", "Home"), "WebHome");
        ViewPage viewPage = setup.gotoPage(importedReference);
        String pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/importGithubWiki/Home.resource");
        File resourceFile = new File(resourceURL.getPath());
        String resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        // pageContent = pageContent + "\n";
        // resourceContent = StringUtils.chop(resourceContent);
        // FileUtils.writeStringToFile(new File("importGithubWikiHome1.txt"), resourceContent, StandardCharsets.UTF_8);
        // FileUtils.writeStringToFile(new File("importGithubWikiHome2.txt"), pageContent, StandardCharsets.UTF_8);
        assertEquals(resourceContent, pageContent);

        importedReference =
            new DocumentReference("xwiki", Arrays.asList("GithubImporterTestParent", "Tests"), "WebHome");
        viewPage = setup.gotoPage(importedReference);
        pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/importGithubWiki/Tests.resource");
        resourceFile = new File(resourceURL.getPath());
        resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);

        importedReference =
            new DocumentReference("xwiki", Arrays.asList("GithubImporterTestParent", "Tests", "Test1"), "WebHome");
        viewPage = setup.gotoPage(importedReference);
        pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/importGithubWiki/Test1.resource");
        resourceFile = new File(resourceURL.getPath());
        resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);

        importedReference =
            new DocumentReference("xwiki", Arrays.asList("GithubImporterTestParent", "Tests", "Test2"), "WebHome");
        viewPage = setup.gotoPage(importedReference);
        pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/importGithubWiki/Test2.resource");
        resourceFile = new File(resourceURL.getPath());
        resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);
    }

    @Test
    @Order(3)
    public void importGithubPages(TestUtils setup, XWikiWebDriver driver)
        throws IOException, InterruptedException
    {
        EntityReference ghImporterReference = new DocumentReference("xwiki", "GitHub Importer", "WebHome");
        setup.gotoPage(ghImporterReference);

        // Select type to File / Directory
        WebElement inputType = driver.findElement(By.name("githubimporter_sourcetype"));
        inputType.click();
        inputType = driver.findElement(By.name("githubimporter_sourcetype_file"));
        inputType.click();
        // Uncheck syntax conversion
        inputType = driver.findElement(By.name("githubimporter_properties_descriptor_convertSyntax"));
        inputType.click();
        // Uncheck create hierarchy
        inputType = driver.findElement(By.name("githubimporter_properties_descriptor_createHierarchy"));
        inputType.click();

        // Get resource url to use it as path for input source
        URL resourceURL = getClass().getResource("/TestRepository");
        WebElement inputElement = driver.findElement(By.id("githubimporter_properties_descriptor_source_file_input"));
        inputElement.sendKeys(resourceURL.getPath());

        inputElement = driver.findElement(By.id("githubimporter_properties_descriptor_parent"));
        inputElement.sendKeys("GithubImporterTestParentNoHierarchyNoConversion");

        // Start conversion
        WebElement submit = driver.findElement(By.name("import"));
        submit.click();

        // @TODO: Use better implementation when available on Filter Module
        Thread.sleep(9 * 1000);

        // Go to the imported pages and assert their content
        EntityReference importedReference = new DocumentReference("xwiki",
            Arrays.asList("GithubImporterTestParentNoHierarchyNoConversion", "Home"), "WebHome");
        ViewPage viewPage = setup.gotoPage(importedReference);
        String pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/TestRepository/Home.md");
        File resourceFile = new File(resourceURL.getPath());
        String resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);

        importedReference = new DocumentReference("xwiki",
            Arrays.asList("GithubImporterTestParentNoHierarchyNoConversion", "CreolePage"), "WebHome");
        viewPage = setup.gotoPage(importedReference);
        pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/TestRepository/CreolePage.creole");
        resourceFile = new File(resourceURL.getPath());
        resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);

        importedReference = new DocumentReference("xwiki",
            Arrays.asList("GithubImporterTestParentNoHierarchyNoConversion", "MediaWikiPage"), "WebHome");
        viewPage = setup.gotoPage(importedReference);
        pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/TestRepository/MediaWikiPage.mediawiki");
        resourceFile = new File(resourceURL.getPath());
        resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);

        importedReference = new DocumentReference("xwiki",
            Arrays.asList("GithubImporterTestParentNoHierarchyNoConversion", "Level1", "Level1Home"), "WebHome");
        viewPage = setup.gotoPage(importedReference);
        pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/TestRepository/Level1/Level1Home.md");
        resourceFile = new File(resourceURL.getPath());
        resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);

        importedReference = new DocumentReference("xwiki",
            Arrays.asList("GithubImporterTestParentNoHierarchyNoConversion", "Level1", "Level2", "Level2Home"),
            "WebHome");
        viewPage = setup.gotoPage(importedReference);
        pageContent = viewPage.editWiki().getContent();
        resourceURL = getClass().getResource("/TestRepository/Level1/Level2/Level2Home.md");
        resourceFile = new File(resourceURL.getPath());
        resourceContent = new String(Files.readAllBytes(resourceFile.toPath()));
        assertEquals(resourceContent, pageContent);
    }
}
