package gpms.selenium;

/*PIuploadsFileAttatchment.java
 * Made by: Nick
 * The PI opens the "upload file" screen then saves the proposal.
 */

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class PIUploadsFileAttatchment {
	private WebDriver driver;
	private String baseUrl;
	private StringBuffer verificationErrors = new StringBuffer();
	private String uploadFile = new String();

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

		String uploadFolderName = "/uploads";
		String uploadLocation = this.getClass().getResource(uploadFolderName)
				.toURI().getPath();
		uploadFile = uploadLocation + "/teapot.jpg";
		uploadFile = uploadFile.replace("/", "\\").substring(1);
	}

	@Test
	public void testPIaddsFileAttatchment() throws Exception {
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

		// File Attachments
		StringSelection st = new StringSelection(uploadFile);

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(st, null);
		Thread.sleep(500);

		((JavascriptExecutor) driver)
				.executeScript("var s=document.getElementById('edit0');s.click();");
		Thread.sleep(500);
		driver.findElement(By.id("lblSection13")).click();
		Thread.sleep(1000);

		driver.findElement(By.cssSelector("div.ajax-file-upload")).click();
		Thread.sleep(500);
		Robot robot = new Robot();

		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);

		Thread.sleep(500);

		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_V);

		robot.keyRelease(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_CONTROL);

		robot.delay(3000);

		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);

		Thread.sleep(500);

		int randTest = (int) (Math.random() * 9999);

		driver.findElement(
				By.xpath(".//*[@id='ui-id-26']/div[2]/div/div[2]/input"))
				.sendKeys("Test" + randTest);

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
		Thread.sleep(1000);
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
