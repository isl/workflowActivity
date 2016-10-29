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

public class FortressDsds  implements JavaDelegate {

	
	 private WebDriver driver;
	 private String baseUrl;
	 private boolean acceptNextAlert = true;
	 private StringBuffer verificationErrors = new StringBuffer();
	    
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		final String host = (String) execution.getVariable("serviceIp");
		final String port = (String) execution.getVariable("port");
		final String selClient1 = (String) execution.getVariable("seleniumClient1");
		String users = (String) execution.getVariable("usersNumber");
		
		// One thread here
		Thread thread1 = new Thread(){
		public void run(){
			final WebDriver driver = setUpLocal(host, port);
			driver.get( baseUrl + "/fortress-web" );
	        login(driver);
	        TUtils.sleep( 1 );
	        workdlowDsds(driver);
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
					remoteDriver1 = setUpRemote(host, port, selClient1);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				remoteDriver1.get( baseUrl + "/fortress-web" );
		        login(remoteDriver1);
		        TUtils.sleep( 1 );
		        workdlowDsds(remoteDriver1);
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
        //driver.findElement( By.linkText( "glob:search*" ) ).click();

	}

	 private void workdlowDsds(WebDriver driver)
	 {
			for(int i=0; i<10; i++){
		        driver.findElement( By.linkText( "DSDS" ) ).click();
		        driver.findElement( By.id( "roleRb" ) ).click();
		        driver.findElement( By.id( GlobalIds.SEARCH_VAL ) ).sendKeys( "oamT13DSD6" );
		        driver.findElement( By.name( GlobalIds.SEARCH ) ).click();
		        TUtils.sleep( 1 );
		        }
			
			
	    	 driver.findElement( By.linkText( "DSDS" ) ).click();
		     driver.findElement( By.id( "roleRb" ) ).click();
		     TUtils.sleep( 4 );
		     driver.findElement( By.linkText( "DSDS" ) ).click();
		     driver.findElement( By.id( "roleRb" ) ).click();
		     
		     driver.findElement( By.linkText( "DSDS" ) ).click();
		     driver.findElement( By.id( "roleRb" ) ).click();
		     TUtils.sleep( 4 );
		     driver.findElement( By.linkText( "DSDS" ) ).click();
		     driver.findElement( By.id( "roleRb" ) ).click();
		     
		     driver.findElement( By.linkText( "DSDS" ) ).click();
		     driver.findElement( By.id( "roleRb" ) ).click();
		     TUtils.sleep( 4 );
		     driver.findElement( By.linkText( "DSDS" ) ).click();
		     driver.findElement( By.id( "roleRb" ) ).click();
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
	

	private final WebDriver setUpRemote(String hostname, String port, String selClient1) throws MalformedURLException {
		// TODO Auto-generated method stub
		  FirefoxProfile ffProfile = new FirefoxProfile();
	        ffProfile.setPreference( "browser.safebrowsing.malware.enabled", false );
	        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
	        final WebDriver driver = new RemoteWebDriver(new URL("http://"+selClient1+":5555/wd/hub"), capabilities);
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
