package gpms.selenium;

/*Made by: Nick
 * Dean logs in and attempts to add a proposal. Fails as dean is not permited to add proposal.
 */

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class ProposalAddFailure {
	private WebDriver driver;
	private String baseUrl;
	private StringBuffer verificationErrors = new StringBuffer();

	@Before
	public void setUp() throws Exception {
		String seleniumDriverFolderName = "/selenium_driver";
		String seleniumDriverLocation = this.getClass()
				.getResource(seleniumDriverFolderName).toURI().getPath();
		System.setProperty("webdriver.chrome.driver", seleniumDriverLocation
				+ File.separator + "chromedriver.exe");
		driver = new ChromeDriver();
		baseUrl = "http://localhost:8181/";
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@Test
	public void testNewProposal() throws Exception {
		driver.get(baseUrl + "GPMS/");
		driver.findElement(By.id("user_email")).clear();
		driver.findElement(By.id("user_email")).sendKeys(
				"deanchemistry1@gmail.com");
		driver.findElement(By.id("user_password")).clear();
		driver.findElement(By.id("user_password")).sendKeys("gpmspassword");
		Thread.sleep(500);
		driver.findElement(By.name("commit")).click();

		assertTrue(driver.findElement(By.cssSelector("BODY")).getText()
				.matches("^[\\s\\S]*$"));
		driver.findElement(By.cssSelector("li.sfLevel1 > a > span")).click();
		driver.findElement(By.id("btnAddNew")).click();
		Thread.sleep(3000);
		assertTrue(driver.findElement(By.cssSelector("BODY")).getText()
				.matches("^[\\s\\S]*$"));
		Thread.sleep(500);
		assertTrue(driver.findElement(By.cssSelector("div.BoxError"))
				.isDisplayed());
		Thread.sleep(500);
		driver.findElement(By.id("BoxAlertBtnOk")).click();
		Thread.sleep(500);
		driver.findElement(By.cssSelector("span.myProfile.icon-arrow-s"))
				.click();
		Thread.sleep(500);
		driver.findElement(By.linkText("Log Out")).click();
		Thread.sleep(2000);
	}

	@After
	public void tearDown() throws Exception {
		driver.quit();
		String verificationErrorString = verificationErrors.toString();
		if (!"".equals(verificationErrorString)) {
			fail(verificationErrorString);
		}
	}

}
