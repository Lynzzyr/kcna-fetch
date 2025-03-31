/*
 * Copyright © 2025 Brandon Namgoong. Licensed under GNU GPLv3.
 * This project is purely for educational and personal purposes.
 */

package com.lynzzyr.kcnafetch.output;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.tinylog.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynzzyr.kcnafetch.Constants.RefineConstants;

/** Class of static methods for performing any video processing and reencoding with ffmpeg. */
public final class Refine {
    /**
     * Create and execute HTTP POST requests to OCR API for an image file
     * @param img The directory of images
     * @return A JSON response
     */
    private static JsonNode postOcrAPI(File img, String apiKey) throws IOException {
        HttpPost req = new HttpPost(RefineConstants.API_URL);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            req.setEntity(MultipartEntityBuilder.create()
                .addTextBody("apikey", apiKey)
                .addTextBody("language", "kor")
                .addTextBody("OCREngine", "2")
                .addPart("file", new FileBody(img, ContentType.IMAGE_JPEG))
                .build()
            );

            HttpClientResponseHandler<String> responseHandler = (ClassicHttpResponse response) -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    return EntityUtils.toString(response.getEntity());
                } else {
                    throw new IOException("Unexpected HTTP status code " + Integer.toString(status));
                }
            };

            Logger.info("HTTP POST request of {} being sent to OCR API...", img.getName());
            String res = client.execute(req, null, responseHandler);

            Logger.info("Response for POST request of {} received", img.getName());
            return new ObjectMapper().readTree(res);
        }
    }

    /**
     * Filters an OCR API JSON result
     * @param result A result
     * @return A valid timestamp String, null if failed
     */
    private static String filterOcrResult(JsonNode result) {
        // filter all failed (exit codes 3 and 4)
        if (
            result.get("OCRExitCode").asInt() == 1 ||
            result.get("OCRExitCode").asInt() == 2
        ) {
            // iterate through array of results
            for (JsonNode res : result.get("ParsedResults")) { 
                if (res
                    .get("ParsedText")
                    .asText()
                    .contains("시") // does not check "분" because may sometimes not appear
                ) {
                    return res.get("ParsedText").asText();
                }
            }
        }

        return null;
    }

    /**
     * Parses raw String timestamps from video. Example "9시 30분"
     * @param rawStrings List of String timestamps
     * @return An ordered List of parsed timestamp integers representing seconds since beginning
     */
    private static List<Integer> parseTimestamps(List<String> rawStrings) {
        List<Integer> timestamps = new ArrayList<>();
        for (String str : rawStrings) {
            // check for precisely the correct format
            Matcher format = Pattern.compile("\\d{1,2}시\\p{Space}\\d{1,2}분").matcher(str);
            if (format.find()) {
                str = format.group();
            } else {
                continue;
            }

            Matcher times = Pattern.compile("\\d+").matcher(str);

            List<Integer> nums = new ArrayList<>();
            while (times.find()) {
                nums.add(Integer.valueOf(times.group()));
            }

            timestamps.add(
                (
                    nums.get(0) * 3600 +
                    (nums.size() == 2 ? nums.get(1) : 0) * 60 // check for minute component
                ) - 32400
            ); // 9 hrs = 32,400 secs
        }

        // final processes
        timestamps = new ArrayList<>(new LinkedHashSet<>(timestamps)); // remove duplicates
        Collections.sort(timestamps);

        return timestamps;
    }

    /**
     * Saves snapshot images of video at each specified frame interval to a new folder within the specified directory.
     * @param file The File to take snapshots of
     * @param tempDir The temporary directory within which will be the folder of images, will also be the working directory for FFmpeg
     * @param start The start timestamp in seconds since beginning
     * @param end The end timestamp in seconds since beginning
     * @param frameInterval How often snapshots are taken using number of frames
     * @param box Optional cropping box for images, specifying width, height and x, y from top left in that order. Pass null for full image
     * @return File to directory of images, null if failed
     */
    public static File saveSnapshots(File file, File tempDir, int start, int end, int frameInterval, int[] box) {
        // create subdir
        File images = tempDir.toPath().resolve(
            file.getName() + "_images_" +
            Integer.toString(start) + "-" +
            Integer.toString(end))
            .toFile();
        images.mkdir();

        // take images
        try {
            ProcessBuilder ffmpeg = new ProcessBuilder(
                "ffmpeg",
                "-i", file.getName(),
                "-ss", Integer.toString(start),
                "-to", Integer.toString(end),
                "-vf",
                    "select=\'not(mod(n," + Integer.toString(frameInterval) + "))\'" +
                    (box != null
                        ? ",crop=" +
                            Integer.toString(box[0]) + ":" +
                            Integer.toString(box[1]) + ":" +
                            Integer.toString(box[2]) + ":" +
                            Integer.toString(box[3])
                        : ""
                    ),
                "-fps_mode", "vfr",
                images.toPath().resolve("image_%04d.jpg").toFile().getAbsolutePath()
            ).directory(tempDir);
            
            int result = ffmpeg.start().waitFor();

            if (result == 0) {
                Logger.info(
                    "Saved images of {} every {} frame{} from {} to {} seconds",
                    file.getName(),
                    frameInterval,
                    frameInterval > 1 ? "s" : "",
                    start,
                    end
                );
                return images;
            } else {
                Logger.warn("FFmpeg exited with non-zero status code!");
                return null;
            }
        } catch (IOException e) {
            Logger.warn("Unexpected IO error occured whilst creating snapshots!");
            return null;
        } catch (InterruptedException e) {
            Logger.warn("Unexpected thread interruption whilst creating snapshots!");
            return null;
        }
    }

    /**
     * Attempts to remove letterboxing on video if occurs the whole way through.
     * @param file The File to fix
     * @param oldAspect The current aspect ratio
     * @param newAspect The new aspect ratio
     * @param force Will ignore letterboxing check and perform fix regardless
     * @return The new File representing a new video file created if fixed, else returns input File
     */
    public static File fixAspectRatio(File file, int[] oldAspect, int[] newAspect, boolean force) {
        return null;
    }

    /**
     * Attempts to search video for timestamps using OCR API.
     * @param file The File to search through
     * @param tempDir The temporary directory, within which would be the File
     * @param apiKey The OCR API key
     * @return List of timestamps in seconds since beginning, null if failed
     * @throws IOException If HTTP status is not OK on request
     */
    public static List<Integer> searchTimestamps(File file, File tempDir, String apiKey) {
        // first pass
        File images = saveSnapshots(
            file,
            tempDir,
            RefineConstants.PRIMARY_START,
            RefineConstants.PRIMARY_END,
            RefineConstants.INTERVAL,
            RefineConstants.BOX
        );
        if (images == null) {
            // log will be made by saveSnapshots
            return null;
        }

        List<String> primary = new ArrayList<>();
        for (File img : images.listFiles()) {
            try {
                String res = filterOcrResult(postOcrAPI(img, apiKey));
                if (res != null) {
                    primary.add(res);
                }
            } catch (IOException e) {
                Logger.warn(e.getMessage());
                break;
            }
        }
        if (!primary.isEmpty()) {
            Logger.info("Timestamps parsed");
            return parseTimestamps(primary);
        }

        // second pass
        images = saveSnapshots(
            file,
            tempDir,
            RefineConstants.LATER_START,
            RefineConstants.LATER_END,
            RefineConstants.INTERVAL,
            RefineConstants.BOX
        );
        if (images == null) {
            return null;
        }

        List<String> later = new ArrayList<>();
        for (File img : images.listFiles()) {
            try {
                String res = filterOcrResult(postOcrAPI(img, apiKey));
                if (res != null) {
                    later.add(res);
                }
            } catch (IOException e) {
                Logger.warn(e.getMessage());
                break;
            }
        }
        if (!later.isEmpty()) {
            Logger.info("Timestamps parsed");
            return parseTimestamps(later);
        }

        // all failed
        return null;
    }

    /**
     * Adds specified timestamps to video.
     * @param file The File to work on
     * @param timestamps List of timestamps in seconds since beginning
     * @return The new File representing a new video file created
     */
    public static File addTimestamps(File file, List<Integer> timestamps) {
        return null;
    }
}
