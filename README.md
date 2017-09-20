# GyazoThumbs
[![Build Status](https://travis-ci.org/Team-Fruit/GyazoThumbs.svg?branch=master)](https://travis-ci.org/Team-Fruit/GyazoThumbs)  
Simple command-line tool to get thumbs from Gyazo

## Requirements
- Java7 or later

## Installation
Create a working folder and place the jar file.

## How It Works
The command-line front-end of Gyazo Thumbs, "GyazoThumbs", download thumbnails of all the images stored in Gyazo. All thumbnails will be JPEG and the image will be degraded, but **this works even if the account is not Pro.** Also, if your account is Pro, you can also download raw images. You can specify the directory to download, number of photos to get, and whether to download only new photos since last downloads.

## Usage
If you don't have a Gyazo access token, create a new application from [Gyazo Applications](https://gyazo.com/oauth/applications) and generate an access token from the details page. Callback URL can be sloppy.

The simplest use is to run from command-line with tokens only. This will download all thumbnails of the authenticated account to the thumbs folder:
```
$ java -jar gyazothumbs-{Version}.jar {Access Token}
```
To download the raw image, use the `-pro` option. (It must be a Gyazo Pro account.):
```
$ java -jar gyazothumbs-{Version}.jar {Access Token} -pro
```
To download only new images, use the `-new` option:
```
$ java -jar gyazothumbs-{Version}.jar {Access Token} -new
```
To specify the number of downloads, add a number to the option:
```
$ java -jar gyazothumbs-{Version}.jar {Access Token} 512
```
To download photos to a directory other than thumbs, use the `-dir` option:
```
$ java -jar gyazothumbs-{Version}.jar {Access Token} -dir /path/to/dir
```
To change the number of download threads, use the `-thread` option. The default is 4:
```
$ java -jar gyazothumbs-{Version}.jar {Access Token} -thread 8
```
All of these options can be mixed and used. example:
```
$ java -jar gyazothumbs-{Version}.jar {Access Token} -dir C:\data\thumbs -new 100
```