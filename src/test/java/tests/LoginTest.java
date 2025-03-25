package tests;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import io.appium.java_client.AppiumBy;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Properties;

public class LoginTest {
        private static AndroidDriver driver;
        private static AppiumDriverLocalService service;
        private static Properties properties;
        private static boolean isCloud = false;

        @BeforeTest
        public static void setup() throws MalformedURLException {
                loadConfig(); // Load properties

                isCloud = detectCloudEnvironment();
                String appiumPath = properties.getProperty("appium.path", "");
                String deviceName = isCloud ? properties.getProperty("cloud.device.name") : properties.getProperty("local.device.name");

                // Start Appium Server
                AppiumServiceBuilder builder = new AppiumServiceBuilder()
                        .withIPAddress("127.0.0.1")
                        .usingPort(4723)
                        .withArgument(GeneralServerFlag.LOG_LEVEL, "debug")
                        .withArgument(GeneralServerFlag.RELAXED_SECURITY)
                        .withLogFile(new File("appium.log"));

                if (!isCloud && !appiumPath.isEmpty()) {
                        builder.withAppiumJS(new File(appiumPath)); // Use local Appium installation
                }

                service = builder.build();
                service.start(); // Start Appium server

                // Set Capabilities
                UiAutomator2Options options = new UiAutomator2Options();
                options.setDeviceName(deviceName);
                options.setPlatformName("Android");
                options.setApp(System.getProperty("user.dir") + "/src/app/VodQA.apk");
                System.out.println("Resolved APK path: " + System.getProperty("user.dir") + "/src/app/VodQA.apk");
                options.setAutomationName("UiAutomator2");
                options.setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(60));


                options.setAppPackage("com.vodqareactnative");
                options.setAppActivity("com.vodqareactnative.MainActivity");

                options.setFullReset(false);
                options.setNoReset(true);


                driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        }

        @Test
        public void testLogin() {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

                WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.accessibilityId("username")));
                usernameField.sendKeys("yourUsername");

                WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.accessibilityId("password")));
                passwordField.sendKeys("yourPassword");

                WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//android.widget.TextView[@text='LOG IN']")));
                loginButton.click();
        }

        @AfterTest
        public static void teardown() {
                if (driver != null) {
                        driver.terminateApp("com.vodqareactnative"); // Stop the app
                        driver.quit();
                }

                if (service != null && service.isRunning()) {
                        service.stop(); // Stop Appium server
                }

                // Ensure no leftover Appium processes
                try {
                        Runtime.getRuntime().exec("taskkill /F /IM node.exe"); // Windows
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }


        // Load configuration from properties file
        private static void loadConfig() {
                properties = new Properties();
                try (InputStream input = new FileInputStream("src/test/resources/config.properties")) {
                        properties.load(input);
                } catch (IOException ex) {
                        ex.printStackTrace();
                        throw new RuntimeException("Failed to load config.properties file.");
                }
        }

        // Detect whether running in Cloud (GCP) or Local
        private static boolean detectCloudEnvironment() {
                try {
                        String hostname = InetAddress.getLocalHost().getHostName();
                        if (hostname.contains("gcp") || hostname.contains("cloud")) {
                                return true; // GCP VM detected based on hostname
                        }
                        String appiumGlobalPath = executeCommand("which appium");
                        return appiumGlobalPath.contains("/usr/bin/appium"); // Typical global Appium path in Linux
                } catch (Exception e) {
                        return false; // Default to local if any error occurs
                }
        }

        // Helper method to execute shell commands
        private static String executeCommand(String command) throws IOException {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                return reader.readLine();
        }
}
