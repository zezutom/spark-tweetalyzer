# Twitter Analysis with Apache Spark

## Contents

- [Get Started](#get-started)
  - [Step 1: Download and build Spark 1.4.1](#step-1-download-and-build-spark-141)
  - [Step 2: Set SPARK_HOME](#step-2-set-spark_home)
  - [Step 3: Build the project](#step-3-build-the-project)
- [Popular Hashtags Counter](#popular-hashtags-counter)
- [Twitter Stream Sentiment Analysis](#twitter-stream-sentiment-analysis)

## Get Started

The application has been tested on [Spark 1.4.1](http://spark.apache.org/releases/spark-release-1-4-1.html) with a built-in [support for Scala 2.11](http://spark.apache.org/docs/latest/building-spark.html#building-for-scala-211).

### Step 1: Download and build Spark 1.4.1

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
The build requires Maven 3 and Java 8: ```cd <YOUR PROJECT DIRECTORY> && mvn clean install```

At this point you should be ready to run the applications described below.

## Popular Hashtags Counter


## Twitter Stream Sentiment Analysis
TODO
