package com.domesticandgeneral.automation.ecommerce.testrunner;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import cucumber.api.testng.AbstractTestNGCucumberTests;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "json:target/cucumber.json", "pretty:target/cucumber-pretty.txt",
        "html:target/cucumber-html-reports" },
        features="classpath:features",
        glue = "com/domesticandgeneral/automation/ecommerce",
        monochrome= false,
        tags={"@test"}

)
public class ecomTest extends AbstractTestNGCucumberTests {
}
