package com.domesticandgeneral.automation.ecommerce.framework;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Hooks {
	private WebUtils utils;
	private static int passedTestCount;
	private static int failedTestCount;
	private static final Logger LOGGER = LoggerFactory.getLogger(Hooks.class);
	//private static PropertyUtil propUtil = PropertyUtil.getInstance();
	//private String API = null;

	private static boolean initialized = false;
	private static final SimpleDateFormat SCREENSHOT_FILENAME_DATE_FORMAT = new SimpleDateFormat(
			"yyyyMMdd'-'HHmmssSSS");

	// WebDriver driver = driverUtil.getDriver();

	public Hooks(WebUtils webUtils) 
	{
		utils = webUtils;
	}

	/**
	 * @author: Mark T
	 * 
	 * @since: sometime in October
	 * @return : void
	 * @Description : Turns off unrecognised SSL warning which stops connection
	 *              to badly setup servers
	 * 
	 * */
	@Before(order = 1)
	public void allowConnectionToBadlySetupServers() {
		// turns off unrecognised SSL warning which stops connection to badly
		// setup servers
		System.setProperty("jsse.enableSNIExtension", "false");
	}

	/**
	 * * @author: Gaurav Seth
	 * 
	 * Updated the method to add the start time in webUtil class
	 * The start time will be used to calculate the elapsed time for any step in that scenario	
	 * 
	 * @since: 5th September 2016
	 * @param : Scenario scenario
	 * @return : void
	 * @Description :This function will print the scenario the test is going to
	 *              execute
	 * 
	 * */
	@Before(order = 2)
	public void before(Scenario scenario)
	{
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Executing scenario -->" + scenario.getName() + "\n");
		}
		utils.setScenario(scenario);
		utils.setStartTimeForScenario(scenario.getName(),  new Date(System.currentTimeMillis()));
		
	}
	
	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 5th September 2016
	 * @param : Scenario scenario
	 * @return : void
	 * @Description :This function will capture the screenshot and embed to the
	 *              failed test
	 * 
	 * */

	@After(order = 5)
	public void captureScreenshot(Scenario scenario)
	{
		if (scenario.isFailed() && utils.getDriver() != null) 
		{
			if (LOGGER.isInfoEnabled()) 
			{
				LOGGER.info("captureScreenshot() - order=4 - capturing the screenshot for the failed test");
			}
			String workingDir = System.getProperty("user.dir");
			String screenshotDir = workingDir
					+ "//target//selenium-test-screenshots//";
			String fileName = (scenario.getName() != null ? ""
					+ scenario.getName().replace(' ', '-') : "")
					+ "_"
					+ SCREENSHOT_FILENAME_DATE_FORMAT.format(new Date())
					+ ".png";

			/*
			 * Moved takeScreenShot code inside WebUtils so that it will be available during execution as well
			 */
			utils.takeScreenShot(screenshotDir, fileName);
		}
	}

	
	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 5th September 2016
	 * @param : Scenario scenario
	 * @return : void
	 * @Description :This function will print the scenario status at the end of
	 *              test execution
	 * 
	 * */

	@After(order = 4)
	public void printScenarioStatus(Scenario scenario) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(" Printing Scenario status below" + "\n");
			LOGGER.info("Hooks() - printScenarioStatus() order=3"
					+ "Scenario with name  -->" + scenario.getName()
					+ " --> has been executed and its status is -->"
					+ scenario.getStatus() + "\n");
			if (scenario.getStatus().equalsIgnoreCase("passed")) {
				passedTestCount = passedTestCount + 1;
			} else if (scenario.getStatus().equalsIgnoreCase("failed")) {
				failedTestCount = failedTestCount + 1;
			}
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("PASSED TEST CASE COUNT -->" + passedTestCount
					+ "  FAILED TEST CASE COUNT -->" + failedTestCount + "\n");
		}
	}

	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 5th September 2016
	 * @param : Scenario scenario
	 * @return : void
	 * @Description :This function will delete the cookies at the end of test
	 *              execution
	 * 
	 * */
	@After(order = 3)
	public void deleteCookiesAfterTestExecution() 
	{
		
		LOGGER.info("deleteCookiesAfterTestExecution() -order=3, Delete all cookies at the end of the test execution");
		//PropertyUtil propUtil = PropertyUtil.getInstance();
		if (utils.getDriver() != null) 
		{
			utils.getDriver().manage().deleteAllCookies();
			LOGGER.info("deleteCookiesAfterTestExecution() Deleted all cookies at the end of the test execution");
		}
	}

	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 5th September 2016
	 * @param : Scenario scenario
	 * @return : void
	 * @Description :This function will close all the browsers opened by the
	 *              WebDriver and also Kill the browser related drivers from the
	 *              processes
	 * 
	 * */

	@cucumber.api.java.Before
	public void setUp() throws Throwable {
		if (!initialized) {
			// Init context. Run just once before first scenario starts

			Runtime.getRuntime().addShutdownHook(new Thread() 
			{
				@Override
				public void run() 
				{

					if (utils.getDriver() != null) 
					{
						LOGGER.info("End of test - closing all Webdriver Browsers");
						try 
						{
							
								utils.getDriver().quit();
								LOGGER.info("Found driver instance in End of test. Killed it");
								utils.killDrivers();
								LOGGER.info("After killing drivers");
						} catch (Throwable e) 
						{
							e.printStackTrace();
						}
						LOGGER.info("End of test - Killing All WebDrivers");
					}
					else
					{
						LOGGER.info("Driver instance was null in End of test. Nothing to kill");
					}
					
					//Stop winium driver service if it's running
					if(utils.getValueFromProperties("browser").equals("winium"))
					{	
						utils.stopWiniumService();
					}
				}
			});

			initialized = true;
		}
	}

	/*
	private void stopWiniumServiceIfRunning() 
	{
		try 
		{
			Process p;
		    String line;
		    String cmd = System.getenv("windir") +"\\system32\\"+"tasklist.exe";
		    p = Runtime.getRuntime().exec(cmd);
		    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    while ((line = input.readLine()) != null) 
		    {
		    	if(line.contains("Winium.Desktop.Driver.exe"))
		    	{	
		    		LOGGER.info(line);
		    		String[] splitcmd = line.split(" ");
		    		LOGGER.info("Process Name : " + splitcmd[0] + " PID: " + splitcmd[4]);
		    		String killCmd = "taskkill /f /pid " + splitcmd[4];
		    		LOGGER.info("Command: " + killCmd);
		    		p = Runtime.getRuntime().exec(killCmd);
		    		BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    		while ((line = output.readLine()) != null) 
				    {
		    			LOGGER.info(line);
				    }
					break;
 		    	}
		    }
		    input.close();
		} 
		catch (Exception err) 
		{
		    err.printStackTrace();
		}
		
	}
	*/
}
