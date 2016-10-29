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

public class FakeAndNonFakeLogIns  implements JavaDelegate {

	
	 private WebDriver driver;
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
	        TUtils.sleep( 1 );
	        FakeAndNonFakeLogIns(driver);
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
		        TUtils.sleep( 1 );
		        FakeAndNonFakeLogIns(remoteDriver1);
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

	}
	
	private void login(String username, String password, WebDriver driver)
    {
        driver.findElement( By.id( GlobalIds.USER_ID ) ).clear();
        driver.findElement( By.id( GlobalIds.USER_ID ) ).sendKeys( username );
        driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).clear();
        driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).sendKeys( password );
        driver.findElement( By.name( GlobalIds.LOGIN ) ).click();
        TUtils.sleep( 1 );
    }
	
	 private void fakeLogin(String username, String password, WebDriver driver) {
    	 
	    	driver.findElement( By.id( GlobalIds.USER_ID ) ).clear();
	        driver.findElement( By.id( GlobalIds.USER_ID ) ).sendKeys( username );
	        driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).clear();
	        driver.findElement( By.id( GlobalIds.PSWD_FIELD ) ).sendKeys( password );
	        driver.findElement( By.name( GlobalIds.LOGIN ) ).click();
	        TUtils.sleep( 2 );
	        driver.findElement( By.name( "relogin") ).click();
		}

	  private void FakeAndNonFakeLogIns(WebDriver driver){
	    		        
	    	driver.get( baseUrl + "/fortress-web" );
	        login("test", "password", driver);
	        driver.findElement( By.linkText( "LOGOUT" ) ).click();
	        TUtils.sleep( 2 );

	        login("test","password",driver);
	        driver.findElement( By.linkText( "LOGOUT" ) ).click();
	        TUtils.sleep( 2 );

	        fakeLogin("test","1212121", driver);
	        TUtils.sleep( 2 );

	        login("test","password", driver);
	        driver.findElement( By.linkText( "LOGOUT" ) ).click();
	        TUtils.sleep( 4 );

	        fakeLogin("test212121","password", driver);
	        TUtils.sleep( 2 );

	        login("test","password", driver);
	        driver.findElement( By.linkText( "LOGOUT" ) ).click();
	        TUtils.sleep( 2 );

	        fakeLogin("21212121","121212222", driver);
	        
	        fakeLogin("password","test", driver);
	        TUtils.sleep( 2 );

	        
	        fakeLogin("damianosMetallidis","1212121212", driver);
	        TUtils.sleep( 2 );

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
			  FirefoxProfile ffProfile = new FirefoxProfile();
		        ffProfile.setPreference( "browser.safebrowsing.malware.enabled", false );
		        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
		        final WebDriver driver = new RemoteWebDriver(new URL("http://192.168.254.134:5555/wd/hub"), capabilities);
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
