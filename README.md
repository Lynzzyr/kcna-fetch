# kcna-fetch

**A needlessly-comprehensive webscraping utility for fetching Korean Central Television broadcast archives**

> _**NOTE:** This is a Java port of [get-kctv](https://github.com/Lynzzyr/get-kctv) as a continuation._

KCNAFetch is a command-line tool that can download a single or range of broadcast archives from the KCNA Watch [KCTV Archive](https://kcnawatch.org/kctv-archive).

`utils.json` contains useful parameters and settings for the program. For example, should KCNA Watch update its KCTV archive database backend and/or video player structure, these changes may be mitigated right from it. Note that it is **required** that the correctly-formatted `utils.json` is present in the directory of `KCNAFetch.jar`.

For usage help run `java -jar kcna-fetch-[VERSION].jar --help`. Requires Java 21+.