package com.lynzzyr.kcnafetch;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.tinylog.Logger;

/** Class for actions related to scraping videos with Selenium Chromedriver. */
public class Fetcher {
    // json things
    JSONObject elements;
    List<String> options;

    // webdriver
    ChromeDriver driver;

    /** Creates a new Fetcher. Chromedriver binary will be handled by Selenium Manager. */
    public Fetcher() {
        // json things
        try (FileReader fr = new FileReader("utils.json")) {
            elements = (JSONObject) ((JSONObject) new JSONParser().parse(fr)).get("elements");
            options = (List<String>) ((JSONObject) new JSONParser().parse(fr)).get("webdriver_opts");
        } catch (IOException e) {
            Logger.error("{} Unexpected IO error! Exiting.", LocalTime.now());
            System.exit(1);
        } catch (ParseException e) {
            Logger.error("{} Unexpected JSON parsing error! Exiting.", LocalTime.now());
            System.exit(1);
        }

        // driver
        driver = new ChromeDriver(new ChromeOptions().addArguments(options));
    }

    /**
     * Creates a new Fetcher.
     * @param executablePath The Chromedriver binary file location
     */
    public Fetcher(String executablePath) {
        // json things
        try (FileReader fr = new FileReader("utils.json")) {
            elements = (JSONObject) ((JSONObject) new JSONParser().parse(fr)).get("urls");
            options = (List<String>) ((JSONObject) new JSONParser().parse(fr)).get("webdriver_opts");
        } catch (IOException e) {
            Logger.error("{} Unexpected IO error! Exiting.", LocalTime.now());
            System.exit(1);
        } catch (ParseException e) {
            Logger.error("{} Unexpected JSON parsing error! Exiting.", LocalTime.now());
            System.exit(1);
        }

        // driver
        driver = new ChromeDriver(new ChromeOptions()
            .addArguments(options)
            .setBinary(Path.of(executablePath).toFile())
        );
    }

    /**
     * Searches for URL of broadcast archive specified.
     * @param date The date of the broadcast
     * @return URL of .mp4 resource
     * @throws NullBroadcastException if broadcast at date does not exist
     */
    public String scrapeUrl(LocalDate date) throws NullBroadcastException {
        Logger.info("{} Starting search for stream URL for {}", LocalTime.now(), date);
        
        // initial search for article
        String u1 = ((String) elements.get("search_urls")).replace(
                "DATE", date.format(DateTimeFormatter.ofPattern((String) elements.get("search_date_pattern")))
            );
        Logger.info("{} {} will be used as the search URL", LocalTime.now(), u1);

        driver.get(u1);

        // find full broadcast from articles
        String u2 = "";

        for (WebElement article : driver.findElements(By.className((String) elements.get("1_class")))) {
            if (article
                .findElement(By.className((String) elements.get("2_class")))
                .getText()
                .equals((String) elements.get("2_element"))
            ) {
                u2 = article
                    .findElement(By.linkText(date.format(DateTimeFormatter.ofPattern((String) elements.get("2_date_pattern")))))
                    .getDomAttribute((String) elements.get("2_attribute"));
                break;
            }
        }

        if (u2.isEmpty()) {
            Logger.error("{} Broadcast of {} does not exist!", LocalTime.now(), date);
            throw new NullBroadcastException("broadcast does not exist");
        } else {
            Logger.info("{} Found full broadcast article at {}", LocalTime.now(), u2);
        }

        driver.get(u2);

        // get final resource url
        String u3 = driver
            .findElement(By.xpath((String) elements.get("3_xpath")))
            .findElement(By.xpath((String) elements.get("4_xpath")))
            .getDomAttribute((String) elements.get("4_attribute"));
        Logger.info("{} Stream URL found at {}!", LocalTime.now(), u3);
        
        // out
        return u3;
    }

    /**
     * Fetches broadcast video from specified URL stream.
     * @param url The URL
     * @param location The directory inside which to save the video
     */
    public void getBroadcast(String url, Path location) {
        
    }
}
