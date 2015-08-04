# Twitter Analysis with Apache Spark

## Contents

- [Get Started](#get-started)
  - [Step 1: Download and build Spark 1.4.1](#step-1-download-and-build-spark-141)
  - [Step 2: Set SPARK_HOME](#step-2-set-spark_home)
  - [Step 3: Build the project](#step-3-build-the-project)
  - [Step 4: Create your OAuth Credentials](#step-4-create-your-oauth-credentials)
  - [Step 5: Externalize App Config](#step-5-externalize-app-config)
- [Popular Hashtags Counter](#popular-hashtags-counter)
- [Twitter Stream Sentiment Analysis](#twitter-stream-sentiment-analysis)

## Get Started

The application has been tested on [Spark 1.4.1](http://spark.apache.org/releases/spark-release-1-4-1.html) with a built-in [support for Scala 2.11](http://spark.apache.org/docs/latest/building-spark.html#building-for-scala-211).

### Step 1: Download and build Spark

Download Spark source package and build it so that it supports Scala 2.11. Follow the instructions [here](http://spark.apache.org/downloads.html) and [here](http://spark.apache.org/docs/latest/building-spark.html#building-for-scala-211).

Please note that one of the dependencies has been removed from the central repository, which makes the build fail. I worked around it by installing the following JAR manually to my local repository.

The [MQTT client library](https://eclipse.org/paho/clients/java/) is no longer present in the Maven Central:
```
<dependency>
  <groupId>org.eclipse.paho</groupId>
  <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
  <version>1.0.1</version>
</dependency>
```

If you get a failed build due to this dependency missing, then download the ZIPped bundle from the link below:

https://repo.eclipse.org/content/repositories/paho-releases/org/eclipse/paho/org.eclipse.paho.client.mqttv3.repository/1.0.1/org.eclipse.paho.client.mqttv3.repository-1.0.1.zip

Unzip it and rename the JAR file:
```
# Whatever your Download directory is, I am using Mac OSX (Mavericks)
$ cd ~/Downloads
$ unzip org.eclipse.paho.client.mqttv3.repository-1.0.1.zip
$ mv plugins/org.eclipse.paho.client.mqttv3_1.0.1.jar plugins/mqttv3_1.0.1.jar
```

Next, install the JAR into your local repository:
```
mvn install:install-file \
  -DgroupId=org.eclipse.paho \
  -DartifactId=org.eclipse.paho.client.mqttv3 \
  -Dpackaging=jar \
  -Dversion=1.0.1 \
  -Dfile=plugins/mqttv3_1.0.1.jar \
  -DgeneratePom=true
```
Now the Spark build should succeed.

### Step 2: Set SPARK_HOME
Once the Spark build succeeds, create (or update if you installed Spark before) a variable called SPARK_HOME. Make it point to the root of the Spark installation directory. See [Spark docs](https://spark.apache.org/docs/latest/quick-start.html) for more details.

Here is the relevant part of my ```~/.bash_profile```

```
export SPARK_HOME=/Users/tom/Programming/spark-1.4.1
```

### Step 3: Build the project
The build requires [SBT](http://www.scala-sbt.org/) (tested on 0.13.8) and Java 8: ```cd <YOUR PROJECT DIRECTORY> && sbt assembly```

At this point you should be ready to run the applications described below.

### Step 4: Create your OAuth Credentials
Register a new app on Twitter in order to get the required credentials (customer key + secret, access token + secret).

### Step 5: Externalize App Config
The app ships with default configuration - see [app.properties](https://github.com/zezutom/spark-tweetalyzer/blob/master/src/main/resources/app.properties). I strongly suggest you do NOT put your actual Twitter credentials into this file. Do the following instead:

First of all, create a new directory outside of this project and associate it with a system variable called TWEETALYZER_CONF_DIR

Here is the relevant part of my ```~/.bash_profile```

```
# Configuration directory for the Spark Tweetalyzer project
export TWEETALYZER_CONF_DIR=/Users/tom/Documents/tweetalyzer/
```

Secondly, copy the default app.properties into the new directory and replace value placeholders with valid Twitter credentials.
 
From now on, the app will read the config from your custom directory and it will only use the packaged config file as a fallback.

## Popular Hashtags Counter
A "hello world" kind of app, showcasing the elementary features of the Spark's Twitter Streaming API. It is based on the official sample called [TwitterPopularTags](https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/streaming/TwitterPopularTags.scala). Unlike the original example, this app reads configuration from a property file and allows to save processing output to a configurable directory - courtesy: [Databricks: Collect a Dataset of Tweets](http://databricks.gitbooks.io/databricks-spark-reference-applications/content/twitter_classifier/collect.html).

Default configuraton:

```
# Spark config
spark.master.url=local[4]

# Twitter credentials
consumer.key=YOUR CONSUMER KEY
consumer.secret=YOUR SECRET KEY
access.token=YOUR ACCESS TOKEN KEY
access.token.secret=YOUR ACCESS TOKEN SECRET

# Streaming context - polling frequency
stream.seconds=2

# How often to seek for the most popular hash tags in "real time"
twitter.tag.seconds=10

# How often to build a "historical" overview of trending hash tags
twitter.tag.history.seconds=60
```

How to run:

1. Package the app (an uber-JAR): ```sbt assembly```
2. Run
```
$SPARK_HOME/bin/spark-submit \
--class "org.zezutom.spark.tweetalyzer.PopularHashTagsCounter" \
target/scala-2.11/tweetalyzer-assembly-0.1.0.jar
```

Here is how to run the integration tests: ```sbt test```

Please note that integration tests run on Spark 1.4.0. That's due to the fact that in the newer version of Spark checkpoints (mandatory) won't work for test streams, which are not serializable.

Apropos, test automation. Yeah, it's been a bit of a challenge due to an asynchronous nature of the streaming. Big thanks to Marcin Kuthan for his [excellent blog post](http://mkuthan.github.io/blog/2015/03/01/spark-unit-testing/) and [invaluable examples on GitHub](https://github.com/mkuthan/example-spark). My implementation follows his approach practically to the letter. Another resource I found very helpful was Holden Karau's [article about Effective testing of Spark programs and jobs](http://strataconf.com/big-data-conference-ny-2015/public/schedule/detail/42993). Her de-facto [framework for Spark's testing](https://github.com/holdenk/spark-testing-base) is definitely worth a check.

## Twitter Stream Sentiment Analysis
TODO
