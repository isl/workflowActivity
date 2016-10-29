package org.workflowMonitoring;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.workflowMonitoring.GlobalIds;
import org.workflowMonitoring.TUtils;

public class FortressUser  implements JavaDelegate {

	
	
	 private String baseUrl;
	 private boolean acceptNextAlert = true;
	 private StringBuffer verificationErrors = new StringBuffer();
	    
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		
		 
		final String host = (String) execution.getVariable("serviceIp");
		final String port = (String) execution.getVariable("port");
		String users = (String) execution.getVariable("usersNumber");
		
		// One thread here
		Thread thread1 = new Thread(){
		public void run(){
			final WebDriver driver = setUpLocal(host, port);
			driver.get( baseUrl + "/fortress-web" );
	        login(driver);
	        TUtils.sleep( 1 );
	    	workflowUsers(driver);
	    	 /*****
	         *  LOGOUT
	         */
	        driver.findElement( By.linkText( "LOGOUT" ) ).click();
	        driver.close();			
			}
		};
		
		// second thread here
		Thread thread2 = new Thread(){
			public void run(){
				WebDriver remoteDriver1 = null;
				try {
					remoteDriver1 = setUpRemote(host, port);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				remoteDriver1.get( baseUrl + "/fortress-web" );
		        login(remoteDriver1);
		        TUtils.sleep( 1 );
		    	workflowUsers(remoteDriver1);
		    	 /*****
		         *  LOGOUT
		         */
		    	remoteDriver1.findElement( By.linkText( "LOGOUT" ) ).click();
		    	remoteDriver1.close();			
				}
			};
		
			if(users.endsWith("1")){
			thread1.start();
			thread1.join();		
			}
			if(users.endsWith("2")){
				thread1.start();
				Thread.sleep(4000);
				thread2.start();

				thread1.join();
				thread2.join();
			}
			
        
        // Second thread here
        //driver.findElement( By.linkText( "glob:search*" ) ).click();

	}

	    private void workflowUsers(WebDriver driver) {
		// TODO Auto-generated method stub
    	  /*****
         *  USERS_PAGE TESTS
         */
        driver.findElement( By.linkText( "USERS" ) ).click();
        //driver.findElement( By.id( "roleRb" ) ).click();
        driver.findElement( By.id( "roleAssignLinkLbl" ) ).click();
        TUtils.sleep( 1 );
        driver.findElement( By.linkText( ">" ) ).click();
        TUtils.sleep( 1 );
        driver.findElement( By.linkText( GlobalIds.SELECT ) ).click();
        TUtils.sleep( 1 );
        driver.findElement( By.name( "userformsearchfields:" + GlobalIds.SEARCH ) ).click();
        driver.findElement( By.id( GlobalIds.FIELD_1 ) ).clear();
        driver.findElement( By.id( GlobalIds.FIELD_1 ) ).sendKeys( "dev1" );
        driver.findElement( By.id( "ouAssignLinkLbl" ) ).click();
        TUtils.sleep( 1 );
        driver.findElement( By.linkText( GlobalIds.SELECT ) ).click();
        TUtils.sleep( 1 );
        driver.findElement( By.name( "userformsearchfields:" + GlobalIds.SEARCH ) ).click();
        TUtils.sleep( 1 );
        WebElement table = driver.findElement(By.id("usertreegrid"));
        List<WebElement> allRows = table.findElements(By.tagName("tr"));
        allRows.get( 4 ).findElement(By.className("imxt-cell")).click();
        TUtils.sleep( 1 );
        allRows.get( 5 ).findElement(By.className("imxt-cell")).click();
        TUtils.sleep( 1 );
        allRows.get( 6 ).findElement(By.className("imxt-cell")).click();
        TUtils.sleep( 1 );
        driver.findElement( By.name( GlobalIds.CLEAR ) ).click();
        TUtils.sleep( 1 );
        driver.findElement( By.id( GlobalIds.USER_ID ) ).sendKeys( "selTestU1" );
        driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).clear();
        driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).sendKeys( "password" );
        driver.findElement( By.id( GlobalIds.OU ) ).clear();
        driver.findElement( By.id( GlobalIds.OU ) ).sendKeys( "dev1" );
        driver.findElement( By.name( GlobalIds.OU_SEARCH ) ).click();
        TUtils.sleep( 2 );
        driver.findElement( By.linkText( GlobalIds.SELECT ) ).click();
        TUtils.sleep( 1 );
        driver.findElement( By.linkText( "USERS" ) ).click();
        TUtils.sleep( 4 );
        driver.findElement( By.linkText( "USERS" ) ).click();
        TUtils.sleep( 4 );
        driver.findElement( By.linkText( "USERS" ) ).click();
        //driver.findElement( By.id( "roleRb" ) ).click();
        TUtils.sleep( 4 );
        driver.findElement( By.name( GlobalIds.ADD ) ).click();
        TUtils.sleep( 1 );
		
	}

	private void login(WebDriver driver) {
		// TODO Auto-generated method stub
		 driver.findElement( By.id( GlobalIds.USER_ID ) ).clear();
	     driver.findElement( By.id( GlobalIds.USER_ID ) ).sendKeys( "test" );
	     driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).clear();
	     driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).sendKeys( "password" );
	     driver.findElement( By.name( GlobalIds.LOGIN ) ).click();
	}

	private final WebDriver setUpLocal(String hostname, String port) {
		// TODO Auto-generated method stub
		  FirefoxProfile ffProfile = new FirefoxProfile();
	        ffProfile.setPreference( "browser.safebrowsing.malware.enabled", false );
	        WebDriver driver = new FirefoxDriver( ffProfile );
	        driver.manage().window().maximize();

	        // tomcat default:
	        baseUrl = "http://"+hostname+":"+port;
	        //baseUrl = "http://fortressdemo2.com:8080";
	        // tomcat SSL:
	        //baseUrl = "https://localhost:8443";
	        //baseUrl = "https://fortressdemo2.com:8443";
	        driver.manage().timeouts().implicitlyWait( 5, TimeUnit.SECONDS );
	        
	        return driver;
		
	}
	

	private final WebDriver setUpRemote(String hostname, String port) throws MalformedURLException {
		// TODO Auto-generated method stub
//		  FirefoxProfile ffProfile = new FirefoxProfile();
//	        ffProfile.setPreference( "browser.safebrowsing.malware.enabled", false );
//	        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
//	        final WebDriver driver = new RemoteWebDriver(new URL("http://192.168.254.134:5555/wd/hub"), capabilities);
//	        driver.manage().window().maximize();

	        FirefoxProfile ffProfile = new FirefoxProfile();
	        ffProfile.setPreference( "browser.safebrowsing.malware.enabled", false );
	        WebDriver driver = new FirefoxDriver( ffProfile );
	        driver.manage().window().maximize();
	        // tomcat default:
	        baseUrl = "http://"+hostname+":"+port;
	        //baseUrl = "http://fortressdemo2.com:8080";
	        // tomcat SSL:
	        //baseUrl = "https://localhost:8443";
	        //baseUrl = "https://fortressdemo2.com:8443";
	        driver.manage().timeouts().implicitlyWait( 5, TimeUnit.SECONDS );
	        
	        return driver;
		
	}

}
