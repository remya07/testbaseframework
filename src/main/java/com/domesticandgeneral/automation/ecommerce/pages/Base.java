package com.domesticandgeneral.automation.ecommerce.pages;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.events.EventFiringWebDriver;

public class Base {
    ChromeOptions options;
    EventFiringWebDriver driver;
    String userPath = System.getProperty("user.dir");

    public EventFiringWebDriver initializeDriver(){
        System.setProperty("webdriver.chrome.driver", userPath + "/src/test/resources/drivers/chromedriver.exe");
        options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new EventFiringWebDriver(new ChromeDriver(options));
        driver.manage().window().maximize();
        return driver;
    }
}
