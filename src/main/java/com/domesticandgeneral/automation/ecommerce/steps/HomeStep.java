package com.domesticandgeneral.automation.ecommerce.steps;

import com.domesticandgeneral.automation.ecommerce.pages.Base;
import cucumber.api.java.en.Given;
import org.openqa.selenium.support.events.EventFiringWebDriver;

public class HomeStep extends Base {

    @Given("^I am on home page$")
    public void i_am_on_home_page() throws Throwable {
        EventFiringWebDriver driver = initializeDriver();
        driver.get("https://www.domesticandgeneral.com");
    }
}
