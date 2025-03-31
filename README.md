# kcna-fetch

**A needlessly-comprehensive webscraping utility for fetching Korean Central Television broadcast archives**

> _**NOTE:** This is a Java port of [get-kctv](https://github.com/Lynzzyr/get-kctv) as a continuation._

KCNAFetch is a command-line tool that can download a single or range of broadcast archives from the KCNA Watch [KCTV Archive](https://kcnawatch.org/kctv-archive).

## Building

Maven is required as a prerequisite to build.

All dependencies and plugins are configured already. To build, clone this repository using `git clone` and run `mvn compile assembly:single`.

## Usage

KCNAFetch uses Java 21+ to run. This is solely a command-line utility; run in a terminal with `java -jar kcna-fetch-[VERSION].jar`. For usage help use `--help`.

If using the `timestamps` option, you must:
- Provide a `.txt` file containing a vaid API key for OCR.SPACE's [OCR API](https://ocr.space/OCRAPI). The location of the text file may be specified with `--ocr_api`.
- Have `ffmpeg` installed and added to the user's PATH.

> _**WARNING:** A free key for OCR.SPACE's free OCR API has a maximum daily limit per IP address of 500 requests. However, please keep in mind that requests may return HTTP 403 codes even before reaching that limit in actual deployment._