/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */

package com.lynzzyr.kcnafetch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tinylog.Logger;

/** Class for scraping videos with Selenium Chromedriver. */
public class Scraper implements AutoCloseable {
    // date
    private LocalDate date;

    // webdriver
    private final ChromeDriver driver;

    // util
    private File lastFile;

    /** Creates a new Scraper. Chromedriver binary will be handled by Selenium Manager. */
    public Scraper(LocalDate date) {
        // date
        this.date = date;

        // driver
        driver = new ChromeDriver(new ChromeOptions().addArguments(
            "--headless=new",
            "disable-infobars",
            "--disable-extensions",
            "--disable-gpu",
            "--disable-dev-shm-usage",
            "--no-sandbox"
        ));
        Logger.debug("Webdriver opened");
    }

    /**
     * Creates a new Scraper.
     * @param binaryPath The Chromedriver binary file location
     */
    public Scraper(LocalDate date, String binaryPath) {
        // date
        this.date = date;

        // driver
        driver = new ChromeDriver(new ChromeOptions().addArguments(
            "--headless=new",
            "disable-infobars",
            "--disable-extensions",
            "--disable-gpu",
            "--disable-dev-shm-usage",
            "--no-sandbox"
        ).setBinary(binaryPath));
        Logger.debug("Webdriver opened");
    }

    /**
     * Prints or refreshes a download progress bar to stdout. Written template by ChatGPT 4o.
     * @param downloaded Number of bytes already downloaded
     * @param totalSize Number of bytes in total
     */
    private static void progressBar(long downloaded, long totalSize) {
        double progress = (double) downloaded / totalSize;
        int filledBars = (int) (progress * 50);
        String bar = "[" + "=".repeat(filledBars) + " ".repeat(50 - filledBars) + "]";
        System.out.print("\033[2K\r" + bar + " " + (int) (progress * 100) + "% (" + downloaded + "/" + totalSize + " bytes) ");
        System.out.flush();
    }

    /**
     * Searches for broadcast archive URL of Scraper instance's current date value.
     * @return URL of .mp4 resource
     * @throws NullBroadcastException if broadcast at date does not exist
     */
    public String scrapeURL() throws NullBroadcastException {
        Logger.info("Starting search for stream URL for {}", date);
        
        // initial search for article
        String u1 = "https://kcnawatch.org/kctv-archive/?start=DATE&end=DATE".replace(
                "DATE",
                date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))
            );
        Logger.info("{} will be used as the search URL", u1);

        driver.get(u1);

        // find full broadcast from articles
        String u2 = "";

        for (WebElement article : driver.findElements(By.className("article-desc"))) {
            if (article
                .findElement(By.className("broadcast-head"))
                .getText()
                .equals("Full Broadcast")
            ) {
                u2 = "https://kcnawatch.org" + article
                    .findElement(By.linkText(date.format(DateTimeFormatter.ofPattern("EEEE LLLL dd, uuuu"))))
                    .getDomAttribute("href");
                break;
            }
        }

        if (u2.isEmpty()) {
            throw new NullBroadcastException("broadcast does not exist");
        } else {
            Logger.info("Found full broadcast article at {}", u2);
        }

        driver.get(u2);

        // get final resource url
        new Actions(driver)
            .moveByOffset(10, 10) // arbitrary
            .perform(); // kcnawatch.org has tried
        String u3 = new WebDriverWait(
            driver,
            Duration.ofSeconds(30) // arbitary
        ).until(
            ExpectedConditions.presenceOfElementLocated(By.xpath("//video[@id = \'bitmovinplayer-video-player\']")))
            .findElement(By.xpath("source"))
            .getDomAttribute("src");
        Logger.info("Stream URL found at {}", u3);
        
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
    public void getBroadcast(
        String url,
        Path dir,
        int timeout,
        boolean temporary,
        boolean replaceExisting
    ) throws URISyntaxException, IOException, SocketTimeoutException {
        // check for save directory
        if (!dir.toFile().isDirectory()) {
            Logger.error("Supplied save location(s) is/are not a directory! Exiting.");
            System.exit(1);
        }
        
        // download file handling
        String fn = temporary
            ? "dl-" + date.toString() + ".mp4"
            : "Broadcast " + date.format(DateTimeFormatter.ofPattern("uuuu MM dd")) + ".mp4";
        File f = lastFile = dir.resolve(fn).toFile();

        // file
        if (f.exists()) {
            if (replaceExisting) {
                f.delete();
                Logger.info("Existing broadcast deleted");
            } else {
                Logger.warn("Broadcast already exists!");
                return;
            }
        }
        
        HttpURLConnection con = (HttpURLConnection) new URI(url).toURL().openConnection();
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);

        // get, will try 3 times in case of download fault
        for (int i = 0; i < 3; i++) {
            Logger.info("File download attempt {}/3", i + 1);

            con.connect();
            Logger.debug("HTTP connection established");

            // verify HTTP status
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP status " + con.getResponseCode());
            }

            // actual data download, certain portions template written by ChatGPT 4o
            byte[] buffer = new byte[8192];
            long dl = 0;
            int read;

            try (
                InputStream istr = con.getInputStream();
                FileOutputStream fos = new FileOutputStream(f);
            ) {
                Logger.info("Getting file...");
                while ((read = istr.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    dl += read;
                    progressBar(dl, con.getContentLengthLong());
                }
                System.out.print("\n"); // newline after progress bar
            }

            con.disconnect();
            Logger.debug("HTTP connection closed");

            // verify full download
            if (dl == con.getContentLengthLong()) {
                Logger.info("Download complete!");
                break;
            } else {
                if (f.exists()) {
                    f.delete();
                    Logger.warn("Failed or incomplete file deleted");
                }
                if (i == 2) {
                    Logger.error("Download failed after 3 attempts!");
                    break;
                } else {
                    Logger.warn("Download failed or incomplete! Retrying.");
                }
            }
        }
    }

    /**
     * Gets the current date used by the Scraper.
     * @return The LocalDate
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Sets the date to be used by the Scraper.
     * @param date The LocalDate
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Gets the last file created by the Scraper.
     * @return The lastFile
     */
    public File getLastFile() {
        return lastFile;
    }

    /**
     * Gets a final filename based on the current instance's date.
     * @return A File
     */
    public String getFinalFileName() {
        return "Full Broadcast " + date.format(DateTimeFormatter.ofPattern("uuuu MM dd")) + ".mp4";
    }

    @Override
    public void close() {
        driver.quit();
    }
}
