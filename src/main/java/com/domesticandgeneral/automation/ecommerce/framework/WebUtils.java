package com.domesticandgeneral.automation.ecommerce.framework;


import com.google.gson.Gson;
import cucumber.api.DataTable;
import cucumber.api.Scenario;
import org.apache.commons.io.FileUtils;
import org.cruk.automation.framework.dataModel.LinkInfo;
import org.junit.Assert;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.winium.WiniumDriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebUtils {

	private DriverUtil driverUtil;
	private static final String TASKLIST = "tasklist";
	private static String KILL = "\\System32\\taskkill /F /IM ";
	private WebDriver driver;
	private PropertyUtil propertyUtil;
	private Properties props;
	private WebDriverWait wait;
	private ExcelDataParser dataParser;
	private Scenario scenario;
	private String envName;
	private String browserName;
	private String version;
	public final String EMAIL = "email";
	public final String TEXT = "text message";
	public final String POST = "post";
	public final String PHONE = "phone";
	public final int ARIA_LABEL = 5;
	public final int ID = 0;
	public final int NAME = 2;
	public final int LINK_TEXT = 3;
	public final int XPATH = 4;
	public final int CSS = 1;
	private int maxWaitinSeconds = 0;
	private HashMap<String, Date> startTimes = new HashMap();
	public static String OS = System.getProperty("os.name").toLowerCase();
	private static final Random generator = new Random();
	private static final Logger LOGGER = LoggerFactory.getLogger(WebUtils.class);
	private String workingDirectory = System.getProperty("user.dir");
	private WiniumDriverService service;
	
	private TestCaseData currentTC = null;
	private List<TestCaseData> allInputData = null;


	private List<TestCaseData> allInputForSingleTC = null;
	
	
	@FindBy(tagName = "button")
	List<WebElement> allButtons;

	@FindBy(tagName = "a")
	List<WebElement> allLinks;

	// Find all labels on page
	@FindBy(tagName = "label")
	List<WebElement> allLabels;

	// Find all error messages on the page
	@FindBy(css = "div [generated='true'].error")
	List<WebElement> allErrors;

	
	@FindBy(name = "payment_card.name")
	WebElement cardHolderName;

	@FindBy(name = "payment_card.number")
	WebElement cardNumber;

	@FindBy(name = "payment_card.expiry_date.month")
	WebElement expDateMonth;

	@FindBy(name = "payment_card.expiry_date.year")
	WebElement expDateYear;
	
	@FindBy(name = "payment_card.start_date.month")
	WebElement startDateMonth;

	@FindBy(name = "payment_card.start_date.year")
	WebElement statrtDateYear;

	@FindBy(name = "payment_card.cvc")
	WebElement cardSecurityCode;

	@FindBy(id = "payNow")
	WebElement payNowBtn;

	@FindBy(id = "creditcard")
	WebElement cardPayment;

	@FindBy(css="input[id='same']")
	WebElement selectSameBillingAddress;
	
	@FindBy(id="paypal")
	WebElement payPal;

	@FindBy(id="payNowPayPal")
	WebElement continuePaypal;

	@FindBy(name="login_email")
	WebElement Email;

	@FindBy(name="login_password")
	WebElement Password;

	@FindBy(id="btnLogin")
	WebElement Login;

	@FindBy(id="confirmButtonTop")
	WebElement Continue;
	
	@FindBy(name="continue")
	WebElement continueButton;

	@FindBy(css="iframe[title='PayPal - Log In']")
	WebElement frame;

	
	public WebDriverWait waitFor() {
		return wait;
	}

	public WebUtils() throws Throwable 
	{
		propertyUtil = new PropertyUtil();
		envName = System.getProperty("env");
		LOGGER.info("Executing tests on: " + envName + " environment.");
		loadProperties(envName);
		checkforPropertiesOverride(); 
		driverUtil = new DriverUtil(props);
		
		String maxWaitTime = getValueFromProperties("MaxWaitTime");
		if(maxWaitTime == null)
		{	
			maxWaitinSeconds = 120;
		}
		else
		{
			maxWaitinSeconds = Integer.parseInt(maxWaitTime);
		}	
		
		if(getDriver() != null)
		{	
			wait = new WebDriverWait(getDriver(), maxWaitinSeconds);
			PageFactory.initElements(getDriver(), this);
		}
		else
		{
			throw new Exception("Web Driver set incorrectly. Stopping the execution.");
		}	
		
		if(getValueFromProperties("browser").equals("winium"))
		{
			startWiniumService();
		}	
	}

	public void startWiniumService() 
	{
		if(service == null)
		{	
			try
			{
				LOGGER.info("Trying to start the WiniumDriver service");
				File driverPath = new File (workingDirectory + "/src/test/resources/drivers/Winium.Desktop.Driver.exe");
				service = new WiniumDriverService.Builder().usingDriverExecutable(driverPath).usingPort(9999).withVerbose(true).buildDesktopService();
				service.start();
				LOGGER.info("Service started");
			}
			catch(Exception ex)
			{
				LOGGER.info("Received exception while starting Winium Driver Service");
				ex.printStackTrace();
			}
		}
		
	}

	public void stopWiniumService()
	{
		if(service != null)
		{	
			service.stop();
			while(service.isRunning())
			{
				waitForSeconds(500);
				LOGGER.info("Service is not yet stopped");
			}	
				
		}
		else
		{
			LOGGER.warn("Winium service was not running.");
		}	
	}
	
	private void checkforPropertiesOverride() 
	{
		Set<Object> allProperties = props.keySet();
		
		for(Object k:allProperties)
		{
			String key = (String)k;
			if(System.getProperty(key) != null)
			{
				props.setProperty(key, System.getProperty(key));
				LOGGER.info("Set property: " + key + " as: " + System.getProperty(key));
			}	
		}	
	}

	public void loadProperties(String envName) throws Throwable 
	{
		props = propertyUtil.getPropertiesForEnvironment(envName); 
		LOGGER.info("Property values: " + props);
	}

	public String getValueFromProperties(String key) {
		return props.getProperty(key);
	}

	public String getRandomAlphaNumericString(int len) {
		String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		SecureRandom rnd = new SecureRandom();

		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}

	public String getRandomAlphaString(int len) {
		String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		SecureRandom rnd = new SecureRandom();

		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}

	public int getAnyRandomIntegerIntheRange(int low, int high) {
		Random random = new Random();
		int randomInt = random.nextInt(high) + low;
		return randomInt;
	}

	public String createdummyEmail() {
		String emailfirstname = getRandomAlphaString(5);
		int suffixemail = getRandomInteger(1000);
		String email = emailfirstname + suffixemail + "@gmail.com";
		return email;
	}

	public static int getRandomInteger(int max) {
		return (int) (Math.random() * max);
	}

	public void fillTextField(WebElement element, String value) 
	{
		if (element != null) 
		{
			try
			{
				waitUntilElementIsVisible(element);
				element.clear();
				element.sendKeys(value);//, Keys.TAB);
			}
			catch(Exception e)
			{
				logToReport(e.getMessage());
			}
		}

	}	
	

	public void clickButton(WebElement element) 
	{
		if (element != null) 
		{
			/*
			if((getValueFromProperties("platform") != null) && (!getValueFromProperties("platform").isEmpty()))
			{	
				waitUntilElementIsClickable(element);
				element.click();
			}
			else if(((getValueFromProperties("browser") != null)) && (getValueFromProperties("browser").equalsIgnoreCase("Safari") || getValueFromProperties("browser").equalsIgnoreCase("Firefox")))
			{
				waitUntilElementIsClickable(element);
				element.click();
			}	
			else
			{
				Actions actions = new Actions(getDriver());
				actions.moveToElement(element);
				waitUntilElementIsClickable(element);
				actions.click().perform();
			}*/
			
			waitUntilElementIsClickable(element);
			element.click();
			
		}
		else {
			LOGGER.debug("clickButton() - Element is null");
			logToReport("clickButton() - Element is null");
		}
	}

	public WebElement waitUntilElementIsClickable(WebElement element) 
	{
		LOGGER.debug("Element id: " + element);
		waitUntilElementIsVisible(element);
		return element;
		
	}

	public void waitForElement(String element) 
	{
		try 
		{
			LOGGER.debug("Waiting for: " + element);
			WebElement ele = null;
			boolean repeat = true;
			float secondsComplete = 0; 
			waitForSeconds(0.500);
			
			while(repeat)
			{	
				secondsComplete+=0.5;
				ele = getElement(element);
				LOGGER.debug("seconds complete: " + secondsComplete + " & waiting for: " + element + " : " + ele);
				if (ele != null || secondsComplete > maxWaitinSeconds )
				{
					repeat = false;
					break;
				}
				else
				{	
					//System.out.println("Element: " + element + " is not yet found");
					Thread.sleep(500);
					//secondsComplete+=0.5;
				}
			}
		} 
		catch (Exception e) 
		{
			LOGGER.debug("Element " + element + " does not exist on page: " + getCurrentUrl());
		}
	}

	/**
	 * This method adds output to cucumber-report rather than printing it to separate logger file
	 * @author Sameer S
	 * @param output
	 * 
	 */
	public void logToReport(String output) 
	{
		getScenario().write(output);
	}
	
	public WebElement waitUntilElementIsVisible(WebElement element) 
	{
		boolean repeat = true;
		float secondsComplete = 0; 
		waitForSeconds(0.500);
		try
		{
			while(repeat)
			{	
				/*
				if(element != null && element.isDisplayed())
				{	
					LOGGER.info("Element " + element.getAttribute("id") + " is visible. ");
				}
				*/
				if (((element != null) && element.isDisplayed()) || (secondsComplete > maxWaitinSeconds))
				{
					repeat = false;
					break;
				}
				else
				{	
					//System.out.println("Element: " + element + " is not yet found");
					Thread.sleep(500);
					secondsComplete+=0.5;
				}
				
				LOGGER.debug("seconds complete: " + secondsComplete + " & maxWaitinSeconds: " + maxWaitinSeconds + " for: " + element);
			}
		}
		catch(Exception ex)
		{
			logToReport("Error received while waiting for element: " + element);
			logToReport(ex.getMessage());
		}
		return element;
	}
	
	
	/**
	 * @param progressBar  Locator 
	 * @return
	 */
	public boolean waitTillProgressBarLoads( final By locator)
	{
		return waitFor().until((ExpectedConditions.invisibilityOfElementLocated(locator)));
	}

	public void selectValueFromDropdown(WebElement element, String value) 
	{
		try 
		{
			if (element != null) 
			{
			
			/*	
				Actions actions = new Actions(getDriver());
				actions.moveToElement(element);
			*/
				Select selElement = new Select(element);
				selElement.selectByVisibleText(value);
			} 
			else 
			{
				logToReport("selectValueFromDropdown() - Found Null element");
			}
		} catch (Exception e) 
		{
			logToReport("Received error while setting value: " + value + " for element: " + element.getText());
			handleException(e, "Received error while setting value: " + value
					+ " for element: " + element.getText());
		}
	}

	public void selectAddressFaster(WebElement ele, String pc, String expAddress)
			throws Exception {
		// driver.findElement(By.id("edit-customer-profile-shipping-commerce-customer-address-und-0-thoroughfare"));
		LOGGER.info("selectAddressFaster() - Looking for address: " + expAddress);
		for (int i = 1; i <= pc.length(); i++) {
			waitUntilElementIsVisible(ele);
			ele.sendKeys(pc.substring((i - 1), i));
			waitForSeconds(0.250);
		}

		// List<WebElement> allAddress =
		// driver.findElements(By.className("pcaitem")); "pcaselected"
		List<WebElement> allAddress = driver.findElements(By
				.className("pcaselected"));
		// System.out.println("Total address found @ First level: " +
		// allAddress.size());
		boolean addrFound = false;
		for (WebElement addr : allAddress) {
			LOGGER.info(" selectAddressFaster() - Found address:  " + addr.getAttribute("title"));
			try {
				// this.waitFor().until(ExpectedConditions.elementToBeSelected(addr));
				if (addr.getAttribute("title").contains(expAddress)) {
					addr.click();
					addrFound = true;
					break;
				}
			} catch (WebDriverException somethingWentWrongTryAgain) {
				LOGGER.info("selectAddressFaster() - " + somethingWentWrongTryAgain.getMessage());
				// selectAddressFaster(ele, pc, expAddress);
			}
		}
		if (!addrFound) {
			throw new Exception("Not able to set the address: " + expAddress);
		}
	}

	public void waitForSeconds(double seconds) {
		try {
			Thread.sleep(new Double((seconds * 1000)).longValue());
		} catch (InterruptedException e) {
			handleException(e, "Wait got interrupted");
		}
	}

	public void enterPassword(WebElement password, String passwordText)
			throws Throwable {

		for (int i = 0; i < passwordText.length(); i++) {
			password.sendKeys(String.valueOf(passwordText.charAt(i)));
			waitForSeconds(0.125);
		}

		waitForSeconds(0.125);
	}

	public String getUniqueString() {
		return String.valueOf(System.currentTimeMillis());
	}

	public void continuePaymentUsingCreditCard() {

		clickButton(cardPayment);
		fillTextField(cardHolderName, "Special Supporter");
		fillTextField(cardNumber, "4111111111111111");
		selectValueFromDropdown(expDateMonth, "03");
		selectValueFromDropdown(expDateYear, "2020");
		if(LOGGER.isDebugEnabled())
		{
			LOGGER.info( "continuePaymentUsingCreditCard() - Setting card exp date as: 03 - 2020 from default method");
		}	
		
		fillTextField(cardSecurityCode, "786");
		selectSameBillingAddress();
		clickButton(payNowBtn);
	}

	private void selectSameBillingAddress() 
	{
		if(selectSameBillingAddress.getAttribute("value").equalsIgnoreCase("false"))
		{
			selectSameBillingAddress.click();
			//System.out.println("selected same billing address");
		}	
		
	}

	public void assertPageURL(String url) {
		Assert.assertEquals(driver.getCurrentUrl(), url);
	}

	public void assertPageContainsElement(WebElement element) {
		Assert.assertTrue(element.isDisplayed());
	}

	public void assertPageContainsText(String text) {
		// wait = new WebDriverWait(driver, 5);
		Assert.assertTrue(driver.getPageSource().contains(text));
	}

	public boolean isPageContainsText(String text) {
		return driver.getPageSource().contains(text);
	}

	public void fillValueForLabel(String label, String value) 
	{
		if(LOGGER.isDebugEnabled())
		{	
			LOGGER.info("fillValueForLabel() - Label name: " + label + " & Value is: " + value);
		}
		
		WebElement element = getElementFromLable(label, "input");
		if (element != null) 
		{
			switch (element.getTagName().toLowerCase()) {
			case "input":
				fillTextField(element, value);
				break;

			case "textarea":
				fillTextField(element, value);
				break;

			case "select":
				selectValueFromDropdown(element, value);
				break;
				
			default:
				if(LOGGER.isInfoEnabled())
				{	
					LOGGER.info("fillValueForLabel() - No Element Type found: " + element.getTagName());
				}
				break;
			}
		} 
		else 
		{
			if(LOGGER.isInfoEnabled())
			{	
				LOGGER.info("fillValueForLabel() - Input element not found for: " + label);
			}
		}
	}

	private WebElement getInputElementForLabel(String label) 
	{
		return getElementFromLable(label, "input");
	}

	public WebElement getErrorElementForLabel(String label) {
		return getElementFromLable(label, "error");
	}

	public WebElement getElementFromLable(String label, String elementType) 
	{
		WebElement element = null;
		String cssLocator = null;
		if(LOGGER.isDebugEnabled())
		{	
			LOGGER.info("getElementFromLable() - Found labels: " + allLabels.size());
		}
		for (WebElement currLabel : allLabels) 
		{
			String labelName = currLabel.getText();

			// If label contains * at end, strip * from label name
			if (labelName.endsWith("*")) {
				labelName = labelName.substring(0, labelName.indexOf('*'));
			}

			if (labelName.trim().toLowerCase().contains(label.trim().toLowerCase())) 
			{
					switch (elementType.toLowerCase()) 
					{
						case "error":
							cssLocator = "label[for=\"" + currLabel.getAttribute("for")
									+ "\"].error";
							break;
		
						case "input":
							cssLocator = "#" + currLabel.getAttribute("for");
							break;
							
						case "select":
							cssLocator = "select[id='" + currLabel.getAttribute("for") + "']";
							break;
					}
				if(LOGGER.isDebugEnabled())
				{	
					LOGGER.info("getElementFromLable() - cssLocator: " + cssLocator);
				}
				element = getDriver().findElement(By.cssSelector(cssLocator));
				
				if(LOGGER.isDebugEnabled())
				{
					LOGGER.info("class string: " + element.getAttribute("class"));
					LOGGER.info("element id: " + element.getAttribute("id"));
				}
				break;
			}
		}
		return element;
	}

	/*
	 * @author Gaurav Seth
	 * 
	 * @since 30th August 2016
	 * 
	 * @param String servicename
	 * 
	 * @Description This function will check if servicename passed to it is
	 * running on the system and accordingly return boolean value
	 */

	public boolean isProcessRunning(String serviceName) throws Exception {

		if ((!isMac()) && (!isLinux())) {
			Process p = Runtime.getRuntime().exec(TASKLIST);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				// System.out.println(line);
				if (line.contains(serviceName)) {
					return true;
				}
			}

		}
		return false;
	}

	public void killDrivers() throws Throwable {

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("killDrivers() - Killing the drivers after test execution");
		}

		if ((!isMac()) && (!isLinux()) ) 
		{
			if (isProcessRunning("IEDriverServer.exe")) 
			{
				killProcess("IEDriverServer.exe");
			} 
			else if (isProcessRunning("chromedriver.exe")) 
			{
				killProcess("chromedriver.exe");
			} else {
				killProcess("geckodriver.exe");
			}
		}
	}

	/*
	 * @author Gaurav Seth
	 * 
	 * @since 30th August 2016
	 * 
	 * @param String servicename
	 * 
	 * @Description This function will kill the service passed to it
	 */

	public static void killProcess(String serviceName) throws Exception {
		KILL = System.getenv("SystemRoot") + KILL;
		Runtime.getRuntime().exec(KILL + serviceName);
	}

	/*
	 * @author Gaurav Seth
	 * 
	 * @since 22th August 2016
	 * 
	 * @param WebElement element
	 * 
	 * @return void
	 * 
	 * @Description This function will check that element is enabled.
	 */

	public void clickWebElementIsEnabled(WebElement element) {
		Assert.assertTrue(element.isEnabled());
	}

	public String getCurrentUrl() {
		return getDriver().getCurrentUrl();
	}

	public void visitPage(String newUrl) 
	{
		if(getDriver() != null)
		{	
			getDriver().get(newUrl);
			LOGGER.info("visitPage() - Visiting page: " + newUrl);
			
			//Overide ssl certificate issue
			WebElement errorMessage = getElementUsingID("overridelink");
			if(errorMessage != null)
			{
				errorMessage.click();
				LOGGER.info("Certificate issue found.");
			}
			/*
			if((getValueFromProperties("device") == null) || (getValueFromProperties("device").isEmpty()))
			{	
				getDriver().manage().window().maximize();
			}
			*/
		}
	}

	private boolean isExpectedError(String errorText, List<String> expectedMessages) {
		boolean isValidErrorMessage = false;

		
		for (String currMessage:expectedMessages) 
		{
			if (currMessage.equals(errorText)) {
				isValidErrorMessage = true;
				break;
			}
		}

		return isValidErrorMessage;
	}

	public String[] populateArray(String inputString) {
		//System.out.println("Original -> " + inputString);
		String newString = inputString.replaceAll("NULL\n", "\n");
		//System.out.println("Post replace -> " + newString);
		String[] stringToArray = newString.split("\\r?\\n");
		//System.out.println("Array size: " + stringToArray.length);
		return stringToArray;
	}

	public boolean validateForm(List<TestCaseData> allTCData, String tabName) 
	{
		boolean allValidationsTrue = true;
		String homePage = driver.getCurrentUrl();
		if(LOGGER.isInfoEnabled())
		{	
			LOGGER.info("validateForm() - No of TCs: " + allTCData.size());
		}
		String[] allColumns = allTCData.get(0).getAllColumns();
		ArrayList<String> expectedErrorMessages = new ArrayList<String>();
		String tcName = null;
		int passCnt = 0;
		int failCnt = 0;

		for (TestCaseData currentTC : allTCData) 
		{
			LOGGER.info("validateForm() - ***** Execution started for TC: " + currentTC.getValueForColumn("TC Scenario"));
			this.visitPage(homePage);
			// Fill all form fields from Spreadsheet except the Expected Error Messages
			expectedErrorMessages.clear();
			if((currentTC.getValueForColumn("Country") != null) && (!currentTC.getValueForColumn("Country").equals("")))
			{
				if(LOGGER.isInfoEnabled())
				{
					LOGGER.info("validateForm() - Setting country first as: " + currentTC.getValueForColumn("Country"));
				}	
				fillValueForLabel("Country", currentTC.getValueForColumn("Country"));
			}
			
			for (int i = 0; i < allColumns.length; i++) 
			{
				String labelName = allColumns[i];
					
				if (labelName.equalsIgnoreCase("Expected Error Messages")) 
				{
					// get the expected error values
					String expectedErrorString = currentTC.getValueForColumn("Expected Error Messages");

					if (expectedErrorString != null) 
					{
						expectedErrorMessages = populateArrayListFromString(expectedErrorString);
					}
					if(LOGGER.isDebugEnabled())
					{	
						if(expectedErrorMessages.isEmpty())
						{
							LOGGER.info("validateForm() - No errors are expected");
						}
						else
						{
							LOGGER.info("validateForm() - Expected " + expectedErrorMessages.size() + " errors");
						}
					}	
				}
				else if (labelName.equalsIgnoreCase("TC Scenario")) 
				{
					tcName = currentTC.getValueForColumn(labelName);
				}
				else if (labelName.equalsIgnoreCase("TC Scenario_description")) 
				{
					//Ignore the description column
				}
				else if (labelName.equalsIgnoreCase("Country"))
				{
					// Don't do anything as already set the country field
				}	
				else 
				{
					String valueForField = currentTC.getValueForColumn(labelName);
					if (valueForField != null && !valueForField.equals("")) 
					{
						fillValueForLabel(labelName, valueForField);
						// Explicitly tab out
						getDriver().switchTo().activeElement().sendKeys(Keys.TAB);
					}
				}
			}

			try {
				boolean currentTCValidation = verifyAllErrorMessages(expectedErrorMessages);
				if (currentTCValidation) 
				{
					if(LOGGER.isInfoEnabled())
					{	
						LOGGER.info("validateForm() - " + tcName + " : Pass");
					}
					currentTC.addValueForColumn("Test Result", "Pass");
					passCnt++;
				} else 
				{
					if(LOGGER.isInfoEnabled())
					{	
						LOGGER.info("validateForm() - " + tcName + " : Fail");
					}
					currentTC.addValueForColumn("Test Result", "Fail");
					String workingDir = System.getProperty("user.dir");
					String screenshotDir = workingDir + "/target/data-validation-screenshots/";
					takeScreenShot(screenshotDir, tcName+""+getDateandTimeAsString()+".png" );
					allValidationsTrue = currentTCValidation;
					failCnt++;
				}
			} catch (Exception ex) 
			{
				handleException(ex, "Error received in ValidateForm method");
			}

		}

		try
		{
			String fileName = "src/test/resources/FormValidation_Output_" + tabName + ".xlsx";
			getDataParser().writeData(fileName, tabName, allTCData);
		}
		catch(Exception ex)
		{
			handleException(ex, "Error while writing the FormValidation output for form: " + tabName);
		}
		
		if(LOGGER.isInfoEnabled())
		{	
			LOGGER.info("*********");

			LOGGER.info("Total TCs executed: " + allTCData.size());
			LOGGER.info("Total TCs pass: " + passCnt);
			LOGGER.info("Total TCs failed: " + failCnt);

			LOGGER.info("*********");
		}
		
		
		return allValidationsTrue;

	}

	
	public ArrayList<String> populateArrayListFromString(String inputString) 
	{
		ArrayList<String> arrayList = new ArrayList<String>();
		
		
		String[] stringArray = inputString.split("\n");
		
		if(stringArray != null)
		{
			for(int i=0; i < stringArray.length; i++)
			{
				arrayList.add(stringArray[i]);
			}
		}
		
		if(LOGGER.isDebugEnabled())
		{	
			LOGGER.info("input string: " + inputString);
			LOGGER.info("List count: " + arrayList.size());
			LOGGER.info("Array count: " + stringArray.length);
		}	
		
		return arrayList;
	}

	public void printTextFromAllElements(List<WebElement> allElements) {
		for (WebElement currElement : allElements) 
		{
			if(LOGGER.isInfoEnabled())
			{	
				LOGGER.info("printTextFromAllElements() - " + currElement.getText());
			}
		}
	}
 	
	public void handleException(Exception e, String message) 
	{
		if(LOGGER.isInfoEnabled())
		{	
			LOGGER.info(message);
			e.printStackTrace();
		}
	}

	public void handleErrors(Throwable t, String message) 
	{
		if(LOGGER.isInfoEnabled())
		{
			LOGGER.info(message + t.getStackTrace());
			t.printStackTrace();
		}
	}

	public boolean verifyAllErrorMessages(List<String> expectedMessages) {
		boolean result = true;
		result = verifyAllErrorMessages(expectedMessages, allErrors);
		return result;
	}

	public boolean verifyAllErrorMessages(List<String> expectedMessages, List<WebElement> actualErrors) 
	{
		boolean result = true;
		LOGGER.info("Actual messages received in the method "+ actualErrors.size());
		if(LOGGER.isDebugEnabled())
		{
			if(actualErrors != null)
			{
				LOGGER.info("verifyAllErrorMessages() - Actul errors found : " + actualErrors.size() + " errors");
			}

			if(expectedMessages != null)
			{
				LOGGER.info("verifyAllErrorMessages() - Expected errorrs found : " + expectedMessages.size() + " errors");
			}
		}

		if (expectedMessages.isEmpty() && actualErrors.isEmpty()) {
			if(LOGGER.isDebugEnabled())
			{
				LOGGER.info("verifyAllErrorMessages() - No errors found");
			}
			result = true;
		}
		else if (expectedMessages.size() == actualErrors.size())
		{
			for (WebElement currentError : actualErrors)
			{
				if (!isExpectedError(currentError.getText(), expectedMessages))
				{
					result = false;
					LOGGER.info("verifyAllErrorMessages() - Unexpected error found: " + currentError.getText());
					break;
				}
				else{
					result = true;
					break;
				}
			}

			if(!result)
			{
				LOGGER.info("############################");
				LOGGER.info("Here are expected errors: ");
				printExpectedErrors(expectedMessages);
				LOGGER.info("############################");
				LOGGER.info("Here are actual errors: ");
				LOGGER.info("****************************");
				printTextFromAllElements(actualErrors);
				LOGGER.info("****************************");
			}
		}
		else
		{
			LOGGER.info("verifyAllErrorMessages() - No of errors are not matching. Expected error messages : " + expectedMessages.size()
							+ " found : "
							+ actualErrors.size());
			LOGGER.info("############################");
			LOGGER.info("Here are expected errors: ");
			printExpectedErrors(expectedMessages);
			LOGGER.info("############################");
			LOGGER.info("Here are actual errors: ");
			LOGGER.info("****************************");
			printTextFromAllElements(actualErrors);
			LOGGER.info("****************************");
			result = false;
		}
		return result;
	}

	private void printExpectedErrors(List<String> expectedMessages)
	{
		if((expectedMessages != null) && (!expectedMessages.isEmpty()))
		{
			for(String currMessage:expectedMessages)
			{
				LOGGER.info(currMessage);
			}
		}
		else
		{
			LOGGER.info("No expected messages to print");
		}
	}

	public void optInFor(String option) {
		String cssLocator = null;
		switch (option.toLowerCase()) {
		case EMAIL:
			cssLocator = "label[for='edit-opt-in-join-email-pyem']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PYEM']";
			}
			break;

		case TEXT:
			cssLocator = "label[for='edit-opt-in-join-text-pysm']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PYSM']";
			}
			break;

		case POST:
			cssLocator = "label[for='edit-opt-in-join-post-pypo']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PYPO']";
			}
			break;

		case PHONE:
			cssLocator = "label[for='edit-opt-in-join-phone-pyph']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PYPH']";
			}

			break;
		}

		clickButton(driver.findElement(By.cssSelector(cssLocator)));

	}

	public void optOutFrom(String option) {
		String cssLocator = null;
		switch (option.toLowerCase()) {
		case EMAIL:
			cssLocator = "label[for='edit-opt-in-join-email-pnem']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PNEM']";
			}
			break;

		case TEXT:
			cssLocator = "label[for='edit-opt-in-join-text-pnsm']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PNSM']";
			}
			break;

		case POST:
			cssLocator = "label[for='edit-opt-in-join-post-pnpo']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PNPO']";
			}
			break;

		case PHONE:
			cssLocator = "label[for='edit-opt-in-join-phone-pnph']";
			if (!isElementFound("css", cssLocator)) {
				cssLocator = "input[value='PNPH']";
			}
			break;
		}

		clickButton(driver.findElement(By.cssSelector(cssLocator)));

	}

	public boolean isElementFound(String locatorTpye, String locator) {
		boolean isElementFound = false;
		WebElement ele = null;
		try {
			switch (locatorTpye.toLowerCase()) {

			case "id":
				ele = driver.findElement(By.id(locator));
				if (ele != null) {
					isElementFound = true;
				}
				break;

			case "css":
				ele = driver.findElement(By.cssSelector(locator));
				if (ele != null) {
					isElementFound = true;
				}
				break;

			case "link":
				ele = driver.findElement(By.linkText(locator));
				if (ele != null) {
					isElementFound = true;
				}
				break;

			case "className":
				ele = driver.findElement(By.className(locator));
				if (ele != null) {
					isElementFound = true;
				}
				break;

			case "name":
				ele = driver.findElement(By.name(locator));
				if (ele != null) {
					isElementFound = true;
				}
				break;

			case "tagName":
				ele = driver.findElement(By.tagName(locator));
				if (ele != null) {
					isElementFound = true;
				}
				break;

			case "xpath":
				ele = driver.findElement(By.xpath(locator));
				if (ele != null) {
					isElementFound = true;
				}
				break;

			}
		} catch (Exception e) {
			isElementFound = false;
		}

		return isElementFound;
	}

	public void verifyAllFieldsAreNotMandatory(List<String> allLabels) {
		boolean isMandatory = false;
		for (String label : allLabels) {
			WebElement ele = getInputElementForLabel(label);
			if (ele.getAttribute("required") != null) {
				isMandatory = true;
				
				if(LOGGER.isInfoEnabled())
				{	
					LOGGER.info("verifyAllFieldsAreNotMandatory() - Expected the field: " + label + " optional but found mandatory");
				}
			}
		}
		Assert.assertFalse(isMandatory);
	}

	public void clickButtonWithLabel(String label) {

		for (WebElement btn : allButtons) {
			if (btn.getText().equals(label)) {
				clickButton(btn);
				break;
			}
		}
	}

	public void verifyALLValuesForFields(Map<String, String> allData) {
		boolean areAllValuesCorrect = true;
		Iterator<String> itr = allData.keySet().iterator();

		while (itr.hasNext()) {
			String label = itr.next();
			if (!isValueInFieldCorrect(getInputElementForLabel(label),
					allData.get(label))) {
				areAllValuesCorrect = false;
			}
		}
		Assert.assertTrue("All Values are not as expected", areAllValuesCorrect);
	}

	public boolean isValueInFieldCorrect(WebElement element,
			String expectedValue) {
		boolean isValueCorrect = false;

		if (element != null) {
			String actualValue = element.getAttribute("value");
			if (actualValue.equals(expectedValue)) {
				isValueCorrect = true;
			} else {
				LOGGER.info("Expected value: " + expectedValue
						+ " found value: " + actualValue);
				isValueCorrect = false;
			}
		} else {
			isValueCorrect = false;
		}

		return isValueCorrect;
	}

	public void verifyPageContains(String searchString) {
		Assert.assertTrue("Expected String not found on page", driver
				.getPageSource().contains(searchString));
	}

	public WebElement getElementUsingCSS(String cssLocator)
	{
		WebElement ele = null;
		if(cssLocator != null)
		{	
			try
			{
				ele = getDriver().findElement(By.cssSelector(cssLocator));
			}
			catch(Exception ex)
			{
				if(LOGGER.isDebugEnabled())
				{	
					LOGGER.info("getElementUsingCSS() - Can not find element with CSS locator: " + cssLocator + " on page: " + getDriver().getCurrentUrl());
				}
			}
		}
		return ele;
	}

	/**
	 * @author Sameer S
	 * @since 22-November-2016
	 * @param xpathExpression
	 * @return WebElement if found by xpath expression else return null
	 */
	
	public WebElement getElementUsingXPATH(String xpathExpression)
	{
		WebElement element = null;
		
		try
		{
			element =  getDriver().findElement(By.xpath(xpathExpression));
		}
		catch(Exception e)
		{
			//handleException(e, "Element not found using xpath: " + xpathExpression);
			if(LOGGER.isDebugEnabled())
			{
				LOGGER.info("getElementUsingXPATH() - Element not found using xpath: " + xpathExpression);
			}	
		}
		return element;
	}
	
	public void selectGenderOption(String option) {
		String cssLocator = null;

		switch (option.toLowerCase()) {
		case "male":
			cssLocator = "input[value='Male']";
			break;

		case "female":
			cssLocator = "input[value='Female']";
			break;
		}
		if(LOGGER.isInfoEnabled())
		{	
			LOGGER.info("selectGenderOption() - css Selector for Gender: " + option + " is : " + cssLocator);
		}
		try 
		{
			executeJavascript("arguments[0].click()", getElementUsingCSS(cssLocator));
			
		} catch (Exception e) {
			handleException(e, "Received Error while chosing gender");
		}
	}


	public void setJoinUsOptions(String email, String text, String post,
			String phone) {
		if (email != null) {
			if (email.equalsIgnoreCase("Yes")) {
				optInFor("email");
			} else {
				optOutFrom("email");
			}
		}

		if (text != null) {
			if (text.equalsIgnoreCase("Yes")) {
				optInFor("text message");
			} else {
				optOutFrom("text message");
			}
		}

		if (post != null) {
			if (post.equalsIgnoreCase("Yes")) {
				optInFor("post");
			} else {
				optOutFrom("post");
			}
		}

		if (phone != null) {
			if (phone.equalsIgnoreCase("Yes")) {
				optInFor("phone");
			} else {
				optOutFrom("phone");
			}
		}
	}

	public String getValueFromTextField(WebElement element) {
		String value = null;
		if (element != null) {
			value = element.getAttribute("value");
		}
		return value;
	}

	public String getExpectedAddress(TestCaseData currentTC) {
		String expectedAddress = currentTC.getValueForColumn("Post Code")
				+ ", " + currentTC.getValueForColumn("Address line 1");

		if (currentTC.getValueForColumn("Address line 2") != null) {
			expectedAddress = expectedAddress + ", "
					+ currentTC.getValueForColumn("Address line 2");
		}

		if (currentTC.getValueForColumn("Address line 3") != null) {
			expectedAddress = expectedAddress + ", "
					+ currentTC.getValueForColumn("Address line 3");
		}

		expectedAddress = expectedAddress + ", "
				+ currentTC.getValueForColumn("City");

		LOGGER.info("getExpectedAddress() - : " + expectedAddress);
		return expectedAddress;
	}

	/*
	private void addAddressManually(TestCaseData currentTC) {
		*
		 * currentTC.getValueForColumn("Country"),
		 * currentTC.getValueForColumn("Address line 1"),
		 * currentTC.getValueForColumn("Address line 2"),
		 * currentTC.getValueForColumn("Address line 3"),
		 * currentTC.getValueForColumn("City"),
		 * currentTC.getValueForColumn("Post Code")
		 
	}
*/
	public WebElement getElementUsingName(String name) 
	{
		WebElement ele = null;
		try
		{
			ele = driver.findElement(By.name(name));
		}
		catch(Exception ex)
		{
			if(LOGGER.isDebugEnabled())
			{	
				LOGGER.info("getElementUsingName() - Can not find element with Name locator: " + name + " on page: " + getDriver().getCurrentUrl());
			}
		}
		return ele;
	}

	public boolean verifyPageDoesNotContain(String searchString) {
		return (!driver.getPageSource().contains(searchString));

	}

	/*
	 * public void closeBrowser() { Set<String> keyset = drivers.keySet();
	 * Iterator<String> iterator = keyset.iterator();
	 * 
	 * while (iterator.hasNext()) { drivers.get(iterator.next()).close(); } }
	 */

	public void fillHiddenField(String fieldName, String value) {
		JavascriptExecutor jse = (JavascriptExecutor) driver;
		String command = "document.getElementsByName(\'" + fieldName
				+ "')[0].value=\'" + value + "\';";
		System.out.println("command: " + command);
		jse.executeScript(command);

	}

	public void clickButtonWithValue(String buttonName) throws Exception {
		String cssLocator = "[value='" + buttonName + "']";
		clickButton(getElementUsingCSS(cssLocator));
	}

	
	public void clickOnLink(String linkName) throws Exception 
	{
		WebElement ele = getElementUsingLinkText(linkName);
		if (ele != null) 
		{
			clickButton(ele);
		} 
		else 
		{
			throw new Exception(linkName + " link not found");
		}
	}

	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 17th August 2016
	 * @param : WebElement element
	 * @Description This function will click on any webElement but use Actions
	 *              class. It takes an WebElement element as parameter and would
	 *              click on that WebElement For Example it can click on button,
	 *              link etc.
	 * 
	 * */
	public void clickWebElementUsingActionsClass(WebElement element) {
		if (element != null) {
			waitUntilElementIsClickable(element);
			Actions actions = new Actions(driver);
			actions.moveToElement(element);
			waitUntilElementIsClickable(element);
			//((JavascriptExecutor) getDriver()).executeScript("$(alert('debug'))");
			actions.perform();
		} else {
			System.out.println("Element is null");
		}
	}

	/*
	 * @author Gaurav Seth
	 * 
	 * @since 17th August 2016
	 * 
	 * @param WebElement element
	 * 
	 * @Description This function will click on any webElement. It takes an
	 * WebElement element as parameter and would click on that WebElement For
	 * Example it can click on button, link etc.
	 */

	public void clickWebElement(WebElement element) {
		if (element != null) 
		{
			//Move towards the element:
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].scrollIntoView();", element);
			
			waitUntilElementIsClickable(element);
			clickButton(element);
		} else {
			if(LOGGER.isInfoEnabled())
			{	
				LOGGER.info("clickWebElement() - Can not click on Element. Element is null");
			}	
			
		}
	}

	/**
	 * @author : Gaurav Seth
	 * @since : 16th August 2016
	 * @param : WebElement element
	 * @return : void Description :This function will clear a webElement passed
	 *         to it
	 **/
	public void clearWebElement(WebElement element) {
		element.click();
		element.clear();
	}

	/**
	 * @author : Gaurav Seth
	 * @since : 16th August 2016
	 * @param : String Expected Text
	 * @return : void Description :This function will check that the text passed
	 *         to it as parameter appears on the webPage. If it does not appears
	 *         the assertion will fail.
	 **/

	public void checkTextAppears(String expectedText) {
		LOGGER.info("checkTextAppears() - Checking the text appears on the page --> " + expectedText);
		List<WebElement> list = driver.findElements(By
				.xpath("//*[contains(text(),\"" + expectedText + "\")]"));
		Assert.assertTrue(list.size() > 0);
	}

	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 6th September 2016
	 * @param : WebElement element
	 * @return : void
	 * @Description :This function will use javascript executor to click on a
	 *              WebElement. It takes WebElement as parameter
	 * 
	 * */

	public void JavaScriptClick(WebElement element) throws Throwable {
		LOGGER.info("JavaScriptClick() - User is using Javascript to click on element" + element);
		try {
			if (element.isEnabled() && element.isDisplayed()) {
				LOGGER.info("JavaScriptClick() - Clicking on element with using java script click");

				((JavascriptExecutor) getDriver()).executeScript(
						"arguments[0].click();", element);
			} else {
				LOGGER.info("JavaScriptClick() - Unable to click on element element is either not enabled or not displayed");
			}
		} catch (StaleElementReferenceException e) {
			LOGGER.info("JavaScriptClick() - Element is not attached to the page document "
					+ e.getStackTrace());
		} catch (NoSuchElementException e) {
			LOGGER.info("JavaScriptClick() - Element was not found in DOM " + e.getStackTrace());
		} catch (Exception e) {
			LOGGER.info("JavaScriptClick() - Unable to click on element " + e.getStackTrace());
		}
	}

	/**
	 * @author: Gaurav Seth
	 * @since: 08th September 2016
	 * @param : WebElement element
	 * @return : void
	 * @Description :This function attempts to click on element multiple times
	 *              to avoid stale element reference exception caused by rapid DOM
	 *              refresh
	 * 
	 * */

	public void dependableClick(WebElement by) {
		final int MAXIMUM_WAIT_TIME = 10;
		final int MAX_STALE_ELEMENT_RETRIES = 5;

		int retries = 0;
		while (true) {
			try {
				wait.until(ExpectedConditions.elementToBeClickable(by)).click();
				return;
			} catch (StaleElementReferenceException e) {
				if (retries < MAX_STALE_ELEMENT_RETRIES) {
					retries++;
					continue;
				} else {
					throw e;
				}
			}
		}
	}

	/**
	 * @author: Gaurav Seth
	 * @since: 08th September 2016
	 * @param : String script, WebElement element
	 * @return : void
	 * @Description :This function will execute javascript on a webelement
	 * 
	 * */

	public void executeJavascript(String script, WebElement element) {
		((JavascriptExecutor) driver).executeScript(script, element);
	}

	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 16th August 2016
	 * @param :String Expected Text
	 * @return : void Description :This function will check that the text passed
	 *         to it as parameter does not appears on the Page.
	 * 
	 * */

	public void checkTextNotAppears(String expectedText) {
		LOGGER.info("checkTextNotAppears() - Checking the text does not appears on the page -->"
				+ expectedText);
		List<WebElement> list = driver.findElements(By
				.xpath("//*[contains(text(),\"" + expectedText + "\")]"));
		Assert.assertTrue(list.size() == 0);
	}

	/**
	 * * @author: Sameer
	 * 
	 * @since: 19th Sep 2016
	 * @param: WebElement
	 * @return: boolean Description: Method checks whether element is visible or
	 *          not. Added this method as WebElement.isDisplayed() throws
	 *          element not found exception in POM pattern
	 * */
	
	public boolean isElementVisible(WebElement element) {
		boolean isVisible = false;

		try {
			if (element != null && element.isDisplayed()) {
				isVisible = true;
				if(LOGGER.isDebugEnabled())
				{	
					LOGGER.info("isElementVisible() - Element " + element.getText() + " is visible");
				}
			}
		} 
		catch (Exception e) 
		{
			if(LOGGER.isInfoEnabled())
			{	
				LOGGER.info("isElementVisible() - Element is not visible");
			}
			isVisible = false;
		}

		return isVisible;
	}
	
	
	/**
	 * * @author: Gaurav Seth
	 * 
	 * @since: 09 November 2016
	 * @param: WebElement
	 * @return: WebElement Description: Return WebElement after waiting for certain seconds. 
	 *      
	 * */
	public WebElement fluentWait(WebElement elementpassed) {
		Wait<WebDriver> wait1 = new FluentWait<WebDriver>(getDriver())
				.withTimeout(maxWaitinSeconds, TimeUnit.SECONDS)
				.pollingEvery(500, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class);
		WebElement element = null;
		element = wait1.until(ExpectedConditions.visibilityOf(elementpassed));
		return element;
	}

	/**
	 * * @author: Aditi
	 * 
	 * @since: 19th Sep 2016
	 * @param: DataTable
	 * @return: TestCaseData Description: We use TestCaseData object to read
	 *          data from excel. By using this function we can convert the
	 *          DataTable object to TestCaseData object
	 * */

	public TestCaseData dataTableToTestCaseDataConvertor(DataTable testData) {
		TestCaseData currentTCData = new TestCaseData();

		// Convert DataTable to List of Map values
		List<Map<String, String>> allTestInput = testData.asMaps(String.class,
				String.class);

		LOGGER.info("dataTableToTestCaseDataConvertor() - No of records: " + allTestInput.size());

		Iterator iterator = allTestInput.get(0).entrySet().iterator();

		while (iterator.hasNext()) 
		{
			Map.Entry pair = (Map.Entry) iterator.next();
			currentTCData.addValueForColumn((String) pair.getKey(),(String) pair.getValue());
			if(LOGGER.isDebugEnabled())
			{
				LOGGER.debug("dataTableToTestCaseDataConvertor() - " + (String) pair.getKey() + " : " + (String) pair.getValue());
			}	
		}

		return currentTCData;
	}

	public String getDateandTimeAsString() 
	{
		GregorianCalendar gcalendar = new GregorianCalendar();
		String date;
		String months[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", 
		         "Oct", "Nov", "Dec"};

		date = gcalendar.get(Calendar.DATE)
				+ months[gcalendar.get(Calendar.MONTH)]
				+ gcalendar.get(Calendar.YEAR)
				+ Integer.toString(gcalendar.get(Calendar.HOUR_OF_DAY))
				+ Integer.toString(gcalendar.get(Calendar.MINUTE))
				+ Integer.toString(gcalendar.get(Calendar.SECOND));
		
		return date;
	}

	private boolean isMac() {

		return (OS.indexOf("mac") >= 0);

	}

	private boolean isLinux() {

		return (OS.indexOf("linux") >= 0);

	}
	
	public WebDriver getDriver() 
	{
		
		if (driver == null) 
		{
			driver = driverUtil.getDriver();
			if(driver == null)
			{	
				LOGGER.info("Webdriver is not set correctly hence closing the test execution.");
			}
			else
			{
				LOGGER.info("In side getDriver and driver is: " + driver);
			}	
			//getDriverNameAndVersion();
		}
		return driver;
	}

	public ExcelDataParser getDataParser() {
		if (dataParser == null) {
			dataParser = new ExcelDataParser();
		}
		return dataParser;
	}

	/*
	public String getDriverNameAndVersion()
	{
		String version = null;
		
		Capabilities caps = (Capabilities) getDriver();
		
		version = caps.getVersion();
		
		LOGGER.info("Browser name: " + caps.getBrowserName() + " and version is: " + version);
		
		return version;
	}
	*/
	public boolean verifyData(TestCaseData actualData, TestCaseData expectedTCData) 
	{
		boolean result = false;
		
		if(expectedTCData.size() == actualData.size())
		{
			if(expectedTCData.equals(actualData))
			{
				result = true;
			}	
		}	
		
		return result;
	}
	
	/**
	 * @author Sameer S
	 * @param screenshotDir
	 * @param fileName
	 * Added this method in WebUtils so that it will be available during the process
	 */
	public void takeScreenShot(String screenshotDir, String fileName) {
		try {
			byte[] screenshot = ((TakesScreenshot)getDriver()).getScreenshotAs(OutputType.BYTES);
			File file = new File(screenshotDir + fileName);
			int i = 0;
			while (file.exists()) {
				file = new File(screenshotDir + fileName + "-" + i++ + ".png");
			}
			FileUtils.writeByteArrayToFile(file, screenshot);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setStartTimeForScenario(String scenarioName, Date startTime) 
	{
		startTimes.put(scenarioName, startTime);
	}
	
	public Date getStartTimeForScenario(String scenarioName)
	{
		Date startTime = null;
		if(startTimes.containsKey(scenarioName))
		{
			startTime =  startTimes.get(scenarioName);
		}	
		
		return startTime;
	}

	public void setScenario(Scenario currentScenario)
	{
		this.scenario = currentScenario;
		if (LOGGER.isInfoEnabled())
		{
			LOGGER.info("setScenario() - Scenario Name: " + scenario.getName());
		}	
	}
	
	public Scenario getScenario()
	{
		return scenario;
	}

	public WebElement getElementUsingLinkText(String entityName) 
	{
		WebElement ele = null;
		if(entityName != null)
		{	try
			{
				ele = getDriver().findElement(By.linkText(entityName));
			}
			catch(Exception ex)
			{
				if(LOGGER.isDebugEnabled())
				{	
					LOGGER.info("Link " + entityName + " not found on page " + getDriver().getCurrentUrl());
				}
			}
		}
		return ele;
	}

	/**
	 * This method searches page for given linkNanme
	 * @author Sameer S
	 * @since 20th October 2016
	 * @param linkName - Link Test
	 * @return true if Page has Link by text given linkName
	 */
	
	public boolean isPageContainsLink(String linkName) 
	{
		boolean isLinkFound = false;
		
		if(getElementUsingLinkText(linkName) != null)
		{
			isLinkFound = true;
		}	

		return isLinkFound;
	}

	public void enterValidPaypalAccount() throws Throwable
	{
		/*
		* There could be 2 scenarios 
		* 1 - First time log in - login button present
		* 2 - Express check out - This is dealt in finally block
		*/

			waitForElement("iframe[title='PayPal - Log In']");
			WebElement iframe = getElementUsingCSS("iframe[title='PayPal - Log In']");
			getDriver().switchTo().frame(iframe);
			
			WebElement submitBtn = getElementUsingCSS("button[type='submit']");
			LOGGER.debug("Button value: " + submitBtn.getAttribute("value"));
			if(submitBtn != null && submitBtn.getAttribute("value").equalsIgnoreCase("Login"))
			{	
				waitUntilElementIsVisible(Email);
				fillTextField(Email, "pp4_1295867496_per@homemadedigital.com");
				fillTextField(Password, "302173950");
				//waitUntilElementIsVisible(Login);
				waitForElement("button[type='submit']");
		                //Thread.sleep(3000);
				submitBtn.click();
				LOGGER.info("enterValidPaypalAccount() - User entered valid login details");
				//waitUntilElementIsDisplayed("div[class*='spinner']");
				waitForElement("input[value='Continue']");
				waitForSeconds(2);
				WebElement continuebtn = getElementUsingCSS("input[value='Continue']");
				continuebtn.click();
				waitForSeconds(5);
				//waitUntilElementIsDisplayed("div[class*='spinner']");
				LOGGER.info("enterValidPaypalAccount() - Clicked Continue Button");
			}
	}
	
	private void waitUntilElementIsDisplayed(String element) 
	{
		try 
		{
			LOGGER.info("Waiting for: " + element + " to disappear");
			WebElement ele = null;
			boolean repeat = true;
			float secondsComplete = 0; 
			waitForSeconds(0.500);
			
			while(repeat)
			{	
				ele = getElement(element);
				
				LOGGER.debug("seconds complete: " + secondsComplete + " & waiting for: " + element + " : " + ele);
				if (ele != null || secondsComplete > maxWaitinSeconds )
				{
					repeat = false;
					break;
				}
				else
				{	
					//System.out.println("Element: " + element + " is not yet found");
					Thread.sleep(500);
					secondsComplete+=0.5;
				}
			}
		} 
		catch (Exception e) 
		{
			LOGGER.error("Element " + element + " does not exist on page: " + getCurrentUrl());
		}
		
	}
	
	
	public void payUsingPaypal()
	{
		LOGGER.info("enterValidPaypalAccount() - Paypal transaction started at: " + getDateandTimeAsString());
		try
		{
			payPalClick();
	        //waitForSeconds(5);
	        try 
	        {
	            enterValidPaypalAccount();
	            LOGGER.info("Needed Paypal account login");
	        	
	        } 
	        catch (Throwable th) 
	        {
	            payPalLoggedIn();
	        	LOGGER.info("User already logged into Paypal");
	        }
		}
		catch(Throwable th)
		{
			LOGGER.error("Received error while making payment using paypal: " + th.getMessage());
		}

		LOGGER.info("enterValidPaypalAccount() - Paypal transaction completed at: " + getDateandTimeAsString());
	}

	public void payPalLoggedIn() throws Throwable 
	{
		LOGGER.info("payPalLoggedIn() - User is on Payment details page");
		//waitUntilElementIsVisible(Continue);
		//clickWebElement(Continue);
		
		//waitUntilElementIsDisplayed("div[class*='spinner']");
		waitForElement("input[value='Continue']");
		waitForSeconds(2);
		WebElement continuebtn = getElementUsingCSS("input[value='Continue']");
		continuebtn.click();
		waitForSeconds(5);
		//waitUntilElementIsDisplayed("div[class*='spinner']");
		LOGGER.info("User entered valid login details");
	}

	public void payPalClick() throws Throwable 
	{
		waitUntilElementIsVisible(payPal);
		clickWebElement(payPal);
		clickWebElement(continuePaypal);
	}
	
	/**
	 * @author Sameer S
	 * @return TestCaseData object
	 * To use the current TestCase Data please use the getTestCaseDataForScenario method from ExcelDataPasrser to set the current TC.
	 */
	
	public TestCaseData getCurrentTC() 
	{
		return currentTC;
	}

	public void setCurrentTC(TestCaseData currentTC) 
	{
		this.currentTC = currentTC;
	}

	public void loadTheTestDataForScenario(String scenarioName, String tabName, String fileName) 
	{
		dataParser = new ExcelDataParser();
		dataParser.loadData(fileName, tabName);
		currentTC = dataParser.getTestCaseDataForScenario(scenarioName);
		if(LOGGER.isInfoEnabled())
		{
			LOGGER.info("loadTheTestDataForScenario() - Current TC has " + currentTC.getAllColumns().length + " columns.");	
		}	
		
	}

	public List<TestCaseData> getAllTC() 
	{
		return allInputData;
	}
	
	public void loadAllTestData(String tabName, String fileName) 
	{
		dataParser = new ExcelDataParser();
		dataParser.loadData(fileName, tabName);
		allInputData = dataParser.getAllData();
		if(LOGGER.isInfoEnabled())
		{
			LOGGER.info("loadAllTestData() -  Total TCs loaded: " + allInputData.size());	
		}	
		
	}
	
	public void selectCodeFromDropdown(WebElement element, String code) 
	{
		try 
		{
			if (element != null) 
			{
				Select selElement = new Select(element);
				selElement.selectByValue(code);
			} else 
			{
				LOGGER.info("selectCodeFromDropdown() - Found Null element in selectValueFromDropdown method");
			}
		} 
		catch (Exception e) 
		{
			handleException(e, "Received error while setting code: " + code
					+ " for element: " + element.getText());
		}	
		
	}

	public WebElement getElementUsingID(String elementId) 
	{
		WebElement ele = null;
		
		try
		{
			if(elementId != null)
			{	
				ele = getDriver().findElement(By.id(elementId));
			}
			
		}
		catch (Exception ex)
		{
			if(LOGGER.isDebugEnabled())
			{
				LOGGER.info("getElementUsingID() - Element not found using element id: " + elementId);
			}	
		}
		
		return ele;
	}

	public String continuePaymentUsingCreditCard(String cardType, String cardName) 
	{
		String cardNbr = null;
		if(cardType == null)
		{
				cardType = "visa";
		}
		
		switch (cardType.toLowerCase()) 
		{
			case "visa":
			case "visacard":	
				cardNbr = "4444333322221111";
				break;
	
			case "mastercard":
			case "master card":
			case "master":	
				cardNbr = "5555555555554444";
				break;
				
			case "maestro":
				cardNbr = "6759649826438453";
				break;
				
			case "jcb":
				cardNbr = "3528000700000000";
				break;	
			
			case "amex":
			case "american express":
				cardNbr = "343434343434343";
				break;
				
			case "visa debit":
				cardNbr = "4917610000000000003";
				break;

			case "visa electron":
				cardNbr = "4917300800000000";
				break;

				
			default:
				cardNbr = "4111111111111111";
				if(LOGGER.isInfoEnabled())
				{
					LOGGER.info("continuePaymentUsingCreditCard() - " + cardType + " not known hence using Visa");
				}	
				break;
		}

		if(cardName == null || cardName.contains("null"))
		{
			cardName = "Special Supporter";
		}	
		
		clickButton(cardPayment);
		fillTextField(cardHolderName, cardName);	
		fillTextField(cardNumber, cardNbr);
		
		// @aditi: In case there is extra space like in maestro card, we need to remove the space explicitly
		if (getValueFromTextField(cardNumber).charAt(getValueFromTextField(cardNumber).length()-1) == ' ')
			cardNumber.sendKeys(Keys.BACK_SPACE);

		
		if(cardType.toLowerCase().equals("maestro"))
		{
			selectValueFromDropdown(startDateMonth, "01");
			selectValueFromDropdown(statrtDateYear, "2017");
			if(LOGGER.isDebugEnabled())
			{
				LOGGER.info( "continuePaymentUsingCreditCard() - Setting card start date as: 01 - 2017 for card type: " + cardType);
			}	
		}	
		selectValueFromDropdown(expDateMonth, "03");
		selectValueFromDropdown(expDateYear, "2020");
		if(LOGGER.isDebugEnabled())
		{
			LOGGER.info( "continuePaymentUsingCreditCard() - Setting card expiry date as: 03 - 2020 for card type: " + cardType);
		}	
		fillTextField(cardSecurityCode, "786");
		selectSameBillingAddress();
		
		if(LOGGER.isDebugEnabled())
		{
			LOGGER.info( "continuePaymentUsingCreditCard() - Payment done by: " + cardName + " using card type: " + cardType + " & card number is: " + cardNbr);
		}	

		
		clickButton(payNowBtn);
		
		return cardNbr;
	}

	public boolean isElementFocussed(WebElement element) 
	{
		boolean isFocused = false;
		
		if(element != null )
		{
			if(element.equals(getDriver().switchTo().activeElement()))
			{
				isFocused = true;
			}
			else
			{
				if(LOGGER.isInfoEnabled())
				{
					LOGGER.info("isElementFocussed() - element: " + element.getText() +  " is null");
				}
			}	
		}	
		else
		{
			if(LOGGER.isInfoEnabled())
			{
				LOGGER.info("isElementFocussed() - element is null");
			}	
		}	
		return isFocused;
	}
	
	/**
	 * @author aditi 
	 * @param web element
	 * @since Feb 2017
	 * @return returns first selected value from dropdown else returns null
	 */
	public String getSelectedValueFromDropdown(WebElement element)
    {
        String value = null;
        try 
        {
            if (element != null)
            {
                Select selElement = new Select(element);
                value = selElement.getFirstSelectedOption().getText();
                
            } 
            else 
            {
                LOGGER.info("getSelectedValueFromDropdown() - Found Null element");
            }
        } catch (Exception ex)
        {
            handleException(ex, "Received exception while getting selected value from dropdown");
        }
        
        return value;
    }

	public void moveToBottomOfPage() 
	{
		JavascriptExecutor jse = ((JavascriptExecutor) getDriver());
		jse.executeScript("window.scrollTo(0, document.body.scrollHeight)");
	}
	
	public void moveToTopOfPage() 
	{
		JavascriptExecutor jse = ((JavascriptExecutor) getDriver());
		jse.executeScript("window.scrollTo(0, -document.body.scrollHeight)");
	}

	public void navigateToRelativeUrl(String url) 
	{
		visitPage(getValueFromProperties("baseUrl") + url);
	}
	
	
	/**
	 * @author aditi 
	 * @param int noOfDays to add
	 * @since Apr 2017
	 * @return date in string format
	 *  getFutureDateSlashSeparatedBy(1) will return string “14/04/2017”
	 */
	
	public String getFutureDateSlashSeparatedBy(int noOfDays)
	{
		GregorianCalendar gcalendar = new GregorianCalendar();

		gcalendar.add(gcalendar.DATE, noOfDays); // Get sysdate+noOfDays		
		
		SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy");
		String date1 = format1.format(gcalendar.getTime());
						
		return date1;
	}
	
	/**
	 * @author aditi 
	 * @param int noOfDays to add
	 * @since Apr 2017
	 * @return date in string format
	 *	getFutureDateSlashSeparatedBy(365) will return string “14-04-2018”
	 */
	
	public String getFutureDateHyphenSeparatedBy(int noOfDays)
	{
		GregorianCalendar gcalendar = new GregorianCalendar();

		gcalendar.add(gcalendar.DATE, noOfDays);
				
		SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
		String date1 = format1.format(gcalendar.getTime());	
				
		return date1;
	}
	
	/**
	 * @author aditi 
	 * @param NA
	 * @since May 2017
	 * @return NA	
	 */
	public void pressEnterKey() 
	{
		getDriver().switchTo().activeElement().sendKeys(Keys.ENTER);
	}

	public WebElement getElement(String locator) 
	{
		WebElement ele = null;
		
		for(int i=0; i<=5; i++)
		{
			LOGGER.debug("Element " + locator + " searching using index: " + i);
			ele = getElementUsingIndex(i,locator);
			
			if(ele != null)
			{
				LOGGER.info("Element " + locator + " found using index: " + i);
				break;
			}	
		}	
		
		/*
		if(ele == null)
		{
			LOGGER.error("Element " + locator + " not found on page: " + getCurrentUrl());
		}
		*/	
		return ele;
	}
	
	public WebElement getElementUsingIndex(int type, String locator) 
	{
		WebElement element = null;
		
		switch (type) 
		{
			case ARIA_LABEL:
				element = getElementUsingCSS("*[aria-label='" + locator + "']");
				break;

			case ID:
				element = getElementUsingID(locator);
				break;
				
			case CSS:
				element = getElementUsingCSS(locator);
				break;
				
			case NAME:
				element = getElementUsingName(locator);
				break;
				
			case LINK_TEXT:
				element = getElementUsingLinkText(locator);
				break;
				
			case XPATH:
				element = getElementUsingXPATH(locator);
				break;	
		}
		
		return element;
	}

	public boolean verifyFieldHasValue(String fieldName, String expectedValue) 
	{
		boolean isValueCorrect = false;
		WebElement field = getElement(fieldName);
		
		if(field != null)
		{
			LOGGER.info("Text: " + field.getText());
			LOGGER.info("Value: " + field.getAttribute("value"));
			if((field.getAttribute("value") != null) && (field.getAttribute("value").contains(expectedValue)))
			{
				
				isValueCorrect = true;
			}	

			if(field.getText().contains(expectedValue))
			{
				isValueCorrect = true;
			}
			
		}
		else
		{
			LOGGER.error("Element: " + fieldName + " not found.");
		}	
		return isValueCorrect;
	}
	
	public void checkRadioButton(String radioBtn)
	{
		String buttonLocator = "label[for='" + radioBtn + "'] input[type='radio']";
		WebElement ele = getElementUsingCSS(buttonLocator);
		if(ele != null)
		{
			clickButton(ele);
		}
		else
		{
			LOGGER.error("Unable to find Radio button: " + radioBtn + " using locator: " + buttonLocator);
		}	
	}

	public void locateTextFieldAndPopulateWithValue(String fieldName, String value) 
	{
		WebElement ele = getElement(fieldName);
		if(ele != null)
		{
			fillTextField(ele, value);
		}	
	}

	public void pressButton(String locator) 
	{
		WebElement ele = null;
		
		String cssLocator = "input[value='" + locator + "']";
		
		ele = getElementUsingCSS(cssLocator);
		
		if(ele == null)
		{
			ele = getElement(locator);
		}	
				
		if(ele != null)
		{
			clickButton(ele);
		}	
	}

	public void loadAllInputsForScenario(String scenarioName, String tabName, String fileName) 
	{
		dataParser = new ExcelDataParser();
		dataParser.loadData(fileName, tabName);
		allInputForSingleTC = dataParser.getAllInputDataForScenario(scenarioName);
		LOGGER.info("loadAllInputsForScenario - Current TC has " + allInputForSingleTC.size() + " rows.");	
	}
	
	public List<TestCaseData> getAllInputForSingleTC() 
	{
		return allInputForSingleTC;
	}

	public BaseModel readJsonFromFile(String fileName, BaseModel baseModel)
	{
		BaseModel model = null;
		try 
		{
			Gson gson = new Gson();
			java.nio.file.Path path = FileSystems.getDefault().getPath(fileName);
			String json = new String(Files.readAllBytes(path));
			model = gson.fromJson(json, baseModel.getClass());
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		return model;
	}

	public boolean verifyAllLinksAreCorrect(List<LinkInfo> links) 
	{	
		boolean areLinksCorrect = true;
		
		List<WebElement> pageLinks = getDriver().findElements(By.tagName("a"));
		
		if(!pageLinks.isEmpty())
		{
			//LOGGER.info("Total links found: " + pageLinks.size());
			for(LinkInfo link:links)
			{
				//LOGGER.info("link text: " + link.getLinkText());
				boolean result = false;
				for(WebElement ele:pageLinks)
				{
					//LOGGER.info("Current Page link: " + ele.getText());
					if(ele.getText().equals(link.getLinkText()))
					{
						String urlstr = ele.getAttribute("href"); 
						if(urlstr.contains(link.getUrl()))
						{
							if(urlstr.startsWith("http"))
							{	
								try 
								{
									URL url = new URL(urlstr);
									HttpURLConnection connection = (HttpURLConnection) url.openConnection();
									connection.setConnectTimeout(60000);
									connection.connect();
									//LOGGER.info("response: " + connection.getResponseCode()); 
									if((connection.getResponseCode() == HttpURLConnection.HTTP_OK)||(connection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED)||(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM)||(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP))
									{	
										result = true;
										pageLinks.remove(ele);
										//LOGGER.info("Links remaining : " + pageLinks.size());
										break;
									}
									else
									{
										LOGGER.info("URL: " + urlstr + " returned status code: " + connection.getResponseCode());
									}	
								} 
								catch (Exception e) 
								{
									LOGGER.info("Received exception while connecting to URL: " + urlstr);
								}
							}
							else if(urlstr.startsWith("mailto:"))
							{
								result = true;
								pageLinks.remove(ele);
								//LOGGER.info("Links remaining : " + pageLinks.size());
								break;
							}
							else
							{
								LOGGER.info("URL neither starts with http nor mailto " + urlstr);
							}	
						}
						else
						{
							LOGGER.info("Link text is correct but expected URL: " + link.getUrl() + " but found url: " + ele.getAttribute("href"));
						}	

					}		
				}
				if(!result)
				{
					areLinksCorrect = false;
					LOGGER.info("Link is not found for link text: " + link.getLinkText());
				}	
			}	
			
		}
		else
		{
			areLinksCorrect = false;
		}	
		return areLinksCorrect;
	}

	public void checkForEmailInMailinator(String emailId) 
	{
		WebElement email = getElementUsingCSS("input[id='inboxfield']");
		if(email != null)
		{
			email.sendKeys(emailId);
			WebElement go = getElementUsingCSS("button[class*='btn-dark']");
			if(go != null)
			{
				go.click();
				waitForElement("th[class*='column-title']");
			}	
		}	
	}
	
	public boolean verifyTextPresentForColumn(String text, String colname) 
	{
		boolean result = false;
		int colId = 0;
		switch (colname.toLowerCase()) 
		{
			case "subject":
				colId = 3;
				break;
	
			case "from":
				colId = 2;
				break;
		}
		
		List<WebElement> rows = getDriver().findElements(By.cssSelector("tr[class*='ng-scope']"));
		
		for(WebElement row:rows)
		{
			List<WebElement> cols = row.findElements(By.tagName("td"));
			if(cols.get(colId).getText().equals(text))
			{
				result = true;
				break;
			}	
			else
			{
				LOGGER.info("Expected: " + text + " found: " + cols.get(colId).getText());
			}	
		}	
		
		return result;

	}
		
	
}

