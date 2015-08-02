package org.zezutom.spark.tweetalyzer

import java.nio.file.Files
import java.util.Properties

import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import twitter4j.Status

import scala.compat.Platform

/**
 * $SPARK_HOME/bin/spark-submit \
 * --class "org.zezutom.spark.tweetalyzer.PopularHashTagsCounter" \
 * target/tweetalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
 */
object PopularHashTagsCounter {

  // Transforms a stream into a hash count map
  def count(stream:DStream[Status], windowDuration: Duration): DStream[(String, Int)] = {
    //stream.saveAsTextFiles("stream_"+ Platform.currentTime)
    stream
      .flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))  // extract hashtags
      .map((_, 1)).reduceByKeyAndWindow(_ + _, windowDuration)
      .map{case (topic, count) => (topic, count)}
      .transform(_.sortBy(pair => pair._2, ascending = false))
  }

  def main(args: Array[String]) {

    // App config
    val config = new Properties()
    config.load(getClass.getResourceAsStream("/app.properties"))

    // Load Twitter credentials
    val (consumerKey, consumerSecret, accessToken, accessTokenSecret) =
      ( config.getProperty("consumer.key"),
        config.getProperty("consumer.secret"),
        config.getProperty("access.token"),
        config.getProperty("access.token.secret"))

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generate OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    // Timing and frequency
    def getSeconds(propName:String): Duration = Seconds(config.getProperty(propName).toLong)

    val (streamSeconds, twitterTagSeconds, twitterTagHistorySeconds) =
      ( getSeconds("stream.seconds"),
        getSeconds("twitter.tag.seconds"),
        getSeconds("twitter.tag.history.seconds"))

    // Spark config
    val masterUrl = config.getProperty("master.url", "local[2]")
    val appName = getClass.getSimpleName
    val checkpointDir = Files.createTempDirectory(appName).toString
    val sparkConf = new SparkConf().setMaster(masterUrl).setAppName(appName)

    val ssc = new StreamingContext(sparkConf, streamSeconds)
    ssc.checkpoint(checkpointDir)

    val stream = TwitterUtils.createStream(ssc, None)

    // Count the most popular hashtags
    val topCounts = count(stream, twitterTagSeconds)
    val topCountsHistory = count(stream, twitterTagHistorySeconds)

    // Output directory
    val outputDir = config.getProperty("output.dir", "tweets")

    // Prints Top 10 topics both to the console and the output directory
    def printTop10(counts:DStream[(String, Int)]) =
      counts.foreachRDD(rdd => {
        val topList = rdd.take(10)
        println(s"\nPopular topics in last $twitterTagHistorySeconds seconds (%s total):".format(rdd.count()))
        topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
        rdd.saveAsTextFile(outputDir + "_" + Platform.currentTime)
      })

    printTop10(topCounts)
    printTop10(topCountsHistory)

    ssc.start()
    ssc.awaitTermination()

  }

}
