package com.lynzzyr.kcnafetch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
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
public class Scraper {
    // date
    private LocalDate date;

    // json things
    private final JSONObject elements;
    private final List<String> options;

    // webdriver
    ChromeDriver driver;

    /** Creates a new Fetcher. Chromedriver binary will be handled by Selenium Manager. */
    public Scraper(LocalDate date) throws IOException, ParseException {
        // date
        this.date = date;

        // json things
        try (FileReader fr = new FileReader("utils.json")) {
            elements = (JSONObject) ((JSONObject) new JSONParser().parse(fr)).get("elements");
            options = (List<String>) ((JSONObject) new JSONParser().parse(fr)).get("webdriver_opts");
        }

        // driver
        driver = new ChromeDriver(new ChromeOptions().addArguments(options));
        Logger.debug("{} Webdriver opened", LocalTime.now());
    }

    /**
     * Creates a new Fetcher.
     * @param executablePath The Chromedriver binary file location
     */
    public Scraper(LocalDate date, String executablePath) throws IOException, ParseException {
        // date
        this.date = date;

        // json things
        try (FileReader fr = new FileReader("utils.json")) {
            elements = (JSONObject) ((JSONObject) new JSONParser().parse(fr)).get("urls");
            options = (List<String>) ((JSONObject) new JSONParser().parse(fr)).get("webdriver_opts");
        }

        // driver
        driver = new ChromeDriver(new ChromeOptions()
            .addArguments(options)
            .setBinary(Path.of(executablePath).toFile())
        );
        Logger.debug("{} Webdriver opened", LocalTime.now());
    }

    /**
     * Searches for URL of broadcast archive specified.
     * @return URL of .mp4 resource
     * @throws NullBroadcastException if broadcast at date does not exist
     */
    public String scrapeUrl() throws NullBroadcastException {
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
        Logger.info("{} Stream URL found at {}", LocalTime.now(), u3);
        
        // out
        return u3;
    }

    /**
     * Fetches broadcast video from specified URL stream.
     * @param url The URL
     * @param dir The directory inside which to save the video
     * @param timeout Timeout in milliseconds for URL connection
     * @param temporary Whether the downloaded file is temporary and needs processing or is going straight to final destination
     * @param replaceExisting Whether to replace an existing download of the same name
     * @throws URISyntaxException
     * @throws IOException If HTTP status is not OK on request
     * @throws SocketTimeoutException if attempt at URL connection exceeds timeout
     */
    public void getBroadcast(String url, Path dir, int timeout, boolean temporary, boolean replaceExisting) throws URISyntaxException, IOException, SocketTimeoutException {
        // check supplied location
        if (!dir.toFile().isDirectory()) {
            Logger.error("{} Supplied save location is not a directory! Exiting.", LocalTime.now());
            System.exit(1);
        }

        // download file handling
        String fn = temporary
            ? "dl-" + date.toString() + ".mp4"
            : "Full Broadcast " + date.format(DateTimeFormatter.ofPattern("uuuu MM dd")) + ".mp4";
        File f = dir.resolve(fn).toFile();

        // file
        if (f.exists()) {
            if (replaceExisting) {
                f.delete();
                Logger.info("{} Existing broadcast deleted", LocalTime.now());
            } else {
                Logger.warn("{} Broadcast already exists!", LocalTime.now());
                return;
            }
        }

        // get
        HttpURLConnection con = (HttpURLConnection) new URI(url).toURL().openConnection();
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
        Logger.debug("{} HTTP connection established", LocalTime.now());

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) { // verify HTTP status
            throw new IOException("HTTP status " + con.getResponseCode());
        }

        try (
            ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());
            FileOutputStream fos = new FileOutputStream(f);
            FileChannel fc = fos.getChannel();
        ) {
            Logger.info("{} Getting data", LocalTime.now());
            fc.transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        Logger.info("{} Fetch complete", LocalTime.now());

        con.disconnect();
        Logger.debug("{} HTTP connection closed", LocalTime.now());
    }

    /**
     * Gets the current date used by the Fetcher.
     * @return The LocalDate
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Sets the date to be used by the Fetcher.
     * @param date The LocalDate
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }
}
