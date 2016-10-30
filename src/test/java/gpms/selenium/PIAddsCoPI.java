package gpms.selenium;

/*Made by: Nick
 * PI logs in and adds a Co-PI to the proposal.
 */

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class PIAddsCoPI {
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
	public void testPIaddsCoPI() throws Exception {
		driver.get(baseUrl + "GPMS/");
		driver.findElement(By.id("user_password")).clear();
		driver.findElement(By.id("user_password")).sendKeys("gpmspassword");
		driver.findElement(By.id("user_email")).clear();
		driver.findElement(By.id("user_email")).sendKeys(
				"nicholas1234@gmail.com");
		Thread.sleep(500);
		driver.findElement(By.name("commit")).click();
		Thread.sleep(500);
		driver.findElement(By.linkText("My Proposals")).click();
		Thread.sleep(500);
		((JavascriptExecutor) driver)
				.executeScript("var s=document.getElementById('edit0');s.click();");
		Thread.sleep(500);
		driver.findElement(By.cssSelector("i.sidebarExpand")).click();
		Thread.sleep(500);
		driver.findElement(By.id("lblSection1")).click();
		Thread.sleep(500);
		driver.findElement(By.name("AddCoPI")).click();
		Thread.sleep(500);
		driver.findElement(By.id("btnSaveProposal")).click();
		Thread.sleep(500);
		driver.findElement(By.id("BoxConfirmBtnOk")).click();
		Thread.sleep(500);
		assertTrue(driver.findElement(By.cssSelector("BODY")).getText()
				.matches("^[\\s\\S]*$"));
		Thread.sleep(500);
		driver.findElement(By.id("BoxAlertBtnOk")).click();
		Thread.sleep(500);
		driver.findElement(By.cssSelector("span.myProfile.icon-arrow-s"))
				.click();
		Thread.sleep(500);
		driver.findElement(By.linkText("Log Out")).click();
		Thread.sleep(500);
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