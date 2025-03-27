# kcna-fetch

**A needlessly-comprehensive webscraping utility for fetching Korean Central Television broadcast archives**

> _**NOTE:** This is a Java port of [get-kctv](https://github.com/Lynzzyr/get-kctv) as a continuation._

KCNAFetch is a command-line tool that can download a single or range of broadcast archives from the KCNA Watch [KCTV Archive](https://kcnawatch.org/kctv-archive).

## Building

Maven is required as a prerequisite to build.

All dependencies and plugins are configured already. To build, clone this repository using `git clone` and run `mvn compile assembly:single`.

## Usage

KCNAFetch requires Java 21+ to run. This is solely a command-line utility; run in a terminal with `java -jar kcna-fetch-[VERSION].jar`. For usage help use `--help`.