package com.domesticandgeneral.automation.ecommerce.framework;

import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Properties;

/**
 * Created by durgakodukulaSameera on 20/09/2016.
 */
public class DriverUtil
{

    private static EventFiringWebDriver driver;
	private Properties props; 
    private String userPath = System.getProperty("user.dir");
    private String osName = System.getProperty("os.name"); 
    private static final Logger LOGGER = LoggerFactory.getLogger(DriverUtil.class);
    private String dockerRemoteDriverUrl = null;
    URL url = null;

    public DriverUtil(Properties envProps)
    {
    	props = envProps;
    	if(!props.getProperty("dockerRemoteDriverUrl").equals(""))
    	{	
    		dockerRemoteDriverUrl = props.getProperty("dockerRemoteDriverUrl");
    	}
    }
    
    
    public void setDriver()
    {
    	DesiredCapabilities capabilities = null;
    	LOGGER.info("Inside set Driver");
        if (driver == null)
        {
        	if(dockerRemoteDriverUrl != null)
    		{	
        		//We are running from browserstack or selenium grid
        		LOGGER.info("dockerRemoteDriverUrl: " + dockerRemoteDriverUrl);
        		capabilities = new DesiredCapabilities();
        		if((props.getProperty("device") != null) && !(props.getProperty("device").isEmpty()))
            	{
        			
            		LOGGER.info("Executing on mobile device: " + props.getProperty("device"));
            		capabilities.setCapability("device", props.getProperty("device"));
            		
            		//capabilities.setCapability("browserName", props.getProperty("browserName"));
            		if(props.getProperty("platform") != null)
            		{
            			capabilities.setCapability("platform", props.getProperty("platform"));
            			if(props.getProperty("platform").equalsIgnoreCase("iOS"))
            			{
            				capabilities.setCapability("browserstack.appium_version", "1.9.1");
            				//capabilities.setCapability("browserstack.safari.driver", "2.48");            			
            			}
            			else
            			{
            				capabilities.setCapability("browserstack.appium_version", "1.6.5");
            			}	
            		}
            		
            		if(props.getProperty("browser") != null)
            		{
            			capabilities.setCapability("browser", props.getProperty("browser"));
            		}
            		
            		if(props.getProperty("realMobile") != null)
            		{
            			capabilities.setCapability("realMobile", props.getProperty("realMobile"));
            		}
            		
            		if(props.getProperty("os") != null)
            		{
            			capabilities.setCapability("os", props.getProperty("os"));
            		}

            		if(props.getProperty("os_version") != null)
            		{
            			capabilities.setCapability("os_version", props.getProperty("os_version"));
            		}
            	}	
        		else
            	{	
            		LOGGER.info("Executing on browser: " + props.getProperty("browser"));
            		capabilities.setCapability("browser", props.getProperty("browser"));
            		if(props.getProperty("browser_version") != null)
            		{
            			capabilities.setCapability("browser_version", props.getProperty("browser_version"));
            		}
            		
            		if(props.getProperty("os") != null)
            		{
            			capabilities.setCapability("os", props.getProperty("os"));
            		}

            		if(props.getProperty("os_version") != null)
            		{
            			capabilities.setCapability("os_version", props.getProperty("os_version"));
            		}
            	}	
        		
        		
        		try
        		{
            		
            		if(dockerRemoteDriverUrl.contains("browserstack"))
            		{	
            			String username = System.getenv("BROWSERSTACK_USER");
            			String accessKey = System.getenv("BROWSERSTACK_ACCESSKEY");
            			String browserstackLocal = System.getenv("BROWSERSTACK_LOCAL");
            			String projectName = null;
            			
            			LOGGER.info("username: " + username + " accessKey: " + accessKey);
            			if(username != null)
            			{
                			url = new URL("https://" + username + ":" + accessKey + "@hub.browserstack.com/wd/hub");
                			LOGGER.info("Running on browserstack from jenkins job: " + url);
            			}	
            			else
            			{
            				//We are executing browserstack job locally.
            				url = new URL(dockerRemoteDriverUrl);
                			LOGGER.info("Running on browserstack from local machine: " + url);
            				browserstackLocal = "true";
            				if(props.getProperty("projectName") != null)
            				{	
            					projectName = props.getProperty("projectName");
            				}
            			}	
            			
	            		capabilities.setCapability("browserstack.local", browserstackLocal);
	            		
	            		if(projectName != null)
	            		{	
	            			capabilities.setCapability("project", projectName);
	            		}

						capabilities.setCapability("acceptInsecureCerts", true);
						capabilities.setCapability("acceptSslCerts",true);
						capabilities.setAcceptInsecureCerts(true);
						capabilities.acceptInsecureCerts();

						if (props.getProperty("BrowserStackConsole").equals("YES")){
							capabilities.setCapability("browserstack.console", "verbose");
						}

	            		driver = new EventFiringWebDriver(new RemoteWebDriver(url, capabilities));
	            		LOGGER.info("Driver set up correctly on Browserstack: " + driver);
            		}
            		else
            		{	
            			url = new URL(dockerRemoteDriverUrl);
            			LOGGER.info("Running on local selenium grid server: " + url);
            			DesiredCapabilities seleniumcapabilities = null;
            			
            			switch (props.getProperty("browser").toLowerCase()) 
            			{
        					case "firefox":
        	            		//System.setProperty("webdriver.gecko.driver", userPath + "/src/test/resources/drivers/Windows/geckodriver.exe");
        						seleniumcapabilities = DesiredCapabilities.firefox();
        						break;
        	
        					case "ie":
        						
                    			seleniumcapabilities = DesiredCapabilities.internetExplorer();
                    			seleniumcapabilities.setBrowserName("internet explorer");
                    			seleniumcapabilities.setPlatform(Platform.WINDOWS);
                    			if(props.getProperty("browser_version") != null)
                    			{	
                    				seleniumcapabilities.setCapability("browser_version", props.getProperty("browser_version"));
                    			}
                    			/*
                    			seleniumcapabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                    			seleniumcapabilities.setCapability("acceptInsecureCerts", true);
                    			seleniumcapabilities.setCapability("acceptSslCerts",true);
                    			seleniumcapabilities.setAcceptInsecureCerts(true);
                    			seleniumcapabilities.acceptInsecureCerts();
                    			*/
        						break;	
        						
        					case "chrome":
        		    			//System.setProperty("webdriver.chrome.driver", userPath + "/src/test/resources/drivers/Windows/chromedriver.exe");
        						//seleniumcapabilities = DesiredCapabilities.chrome();
        						ChromeOptions options = new ChromeOptions();
    							options.addArguments("--no-sandbox");
    					        options.addArguments("--disable-dev-shm-usage");
    							//driver = new EventFiringWebDriver(new ChromeDriver(options));
        						driver = new EventFiringWebDriver(new RemoteWebDriver(url, options)); 
    							break;
        				}
            			
            			
            			if(driver != null)
            			{
            				driver.manage().window().maximize();
            			}
            			else
            			{
            				driver = new EventFiringWebDriver(new RemoteWebDriver(url, seleniumcapabilities));
            			}	
            		}	
        		}
        		catch(Exception ex)
        		{
        			ex.printStackTrace();
        		}
    		}
    		else
    		{
    			//check for os 
    			if(osName.toLowerCase().contains("windows"))
    			{	
	    			LOGGER.info("We are running on Windows Machine and on browser: " +  props.getProperty("browser"));
	    			LOGGER.info("User Path: " + userPath);
	    			ChromeOptions options;
	    			switch (props.getProperty("browser").toLowerCase()) 
	    			{
						case "firefox":
		            		System.setProperty("webdriver.gecko.driver", userPath + "/src/test/resources/drivers/Windows/geckodriver.exe");
				            driver = new EventFiringWebDriver(new FirefoxDriver());
				            LOGGER.info("Firefox driver initiated: " + driver); 
							break;
		
						case "ie":
		        			System.setProperty("webdriver.ie.driver", userPath + "/src/test/resources/drivers/Windows/IEDriverServer.exe");
		    	            driver = new EventFiringWebDriver(new InternetExplorerDriver());
							break;	
							
						case "chrome":
			    			System.setProperty("webdriver.chrome.driver", userPath + "/src/test/resources/drivers/Windows/chromedriver.exe");
							options = new ChromeOptions();
							options.addArguments("--no-sandbox");
					        options.addArguments("--disable-dev-shm-usage");
							driver = new EventFiringWebDriver(new ChromeDriver(options));
							break;
							
						case "headless":
							System.setProperty("webdriver.chrome.driver", userPath + "/src/test/resources/drivers/Windows/chromedriver.exe");
							options = new ChromeOptions();
							options.setHeadless(true);
							options.addArguments("--no-sandbox");
					        options.addArguments("--disable-dev-shm-usage");
							driver = new EventFiringWebDriver(new ChromeDriver(options));
							break;
							
						case "winium":
							LOGGER.info("We are running desktop automation hence headless driver will be initiated");
							System.setProperty("webdriver.chrome.driver", userPath + "/src/test/resources/drivers/Windows/chromedriver.exe");
							ChromeOptions options1 = new ChromeOptions();
							options1.setHeadless(true);
							driver = new EventFiringWebDriver(new ChromeDriver(options1));
							break;
							
					}
	    			if(driver != null)
	    			{
	    				driver.manage().window().maximize();
	    			}
    			}
    			else
    			{
    				LOGGER.info("We are running on os: " + osName + " on browser: " +  props.getProperty("browser"));
	    			LOGGER.info("Drivers should be availabel in the folder: /usr/bin/" );
	    			ChromeOptions options;
	    			switch (props.getProperty("browser").toLowerCase()) 
	    			{
						case "firefox":
		            		System.setProperty("webdriver.gecko.driver", "/usr/bin/geckodriver");
				            driver = new EventFiringWebDriver(new FirefoxDriver());
				            LOGGER.info("Firefox driver initiated: " + driver); 
							break;
		
						case "chrome":
							System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
							options = new ChromeOptions();
							options.addArguments("--no-sandbox");
					        options.addArguments("--disable-dev-shm-usage");
							driver = new EventFiringWebDriver(new ChromeDriver(options));
							break;
							
						case "headless":
							System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
							options = new ChromeOptions();
							options.setHeadless(true);
							options.addArguments("--no-sandbox");
					        options.addArguments("--disable-dev-shm-usage");
							driver = new EventFiringWebDriver(new ChromeDriver(options));
							break;	
					}
    			}

    		}	

        	LOGGER.info("Set driver: " + driver);
        }
    }

	public EventFiringWebDriver getDriver()
    {
   		setDriver();
        return driver;
    }

    public void closeDriver()
    {
        driver.quit();
    }

}
