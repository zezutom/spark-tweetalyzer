package org.zezutom.spark.tweetalyzer

import java.util.Properties

import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.compat.Platform

/**
 * $SPARK_HOME/bin/spark-submit \
 * --class "org.zezutom.spark.tweetalyzer.PopularHashTagsCounter" \
 * target/tweetalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
 */
object PopularHashTagsCounter {

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
    def getSeconds(propName:String): Long = config.getProperty(propName).toLong

    val (streamSeconds, twitterTagSeconds, twitterTagHistorySeconds) =
      ( getSeconds("stream.seconds"),
        getSeconds("stream.seconds"),
        getSeconds("stream.seconds"))

    // Spark config
    val masterUrl = config.getProperty("master.url", "local[2]")
    val sparkConf = new SparkConf().setMaster(masterUrl).setAppName("TwitterPopularTags")
    val ssc = new StreamingContext(sparkConf, Seconds(streamSeconds))
    val stream = TwitterUtils.createStream(ssc, None)

    // Resolve hash tags
    val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))

    // Count occurrences
    def count(seconds:Long): DStream[(Int, String)] =
      hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(seconds))
        .map{case (topic, count) => (count, topic)}
        .transform(_.sortByKey(ascending = false))
    val topCounts = count(twitterTagSeconds)
    val topCountsHistory = count(twitterTagHistorySeconds)

    // Output directory
    val outputDir = config.getProperty("output.dir", "tweets")

    // Prints Top 10 topics both to the console and the output directory
    def printTop10(counts:DStream[(Int, String)]) =
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
