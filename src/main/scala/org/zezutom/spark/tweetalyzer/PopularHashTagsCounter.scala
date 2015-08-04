package org.zezutom.spark.tweetalyzer

import java.nio.file.{Files, Paths}
import java.util.Properties

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import scala.compat.Platform
import scala.io.Source

/**
 * $SPARK_HOME/bin/spark-submit \
 * --class "org.zezutom.spark.tweetalyzer.PopularHashTagsCounter" \
 * target/scala-2.11/tweetalyzer-assembly-0.1.0.jar
 */
object PopularHashTagsCounter extends LazyLogging {

  // Transforms a stream into a hash count map
  def count(stream:DStream[String], windowDuration: Duration): DStream[(String, Int)] =
    stream
      .flatMap(text => text.split(" ").filter(_.startsWith("#")))   // extract hashtags
      .map((_, 1)).reduceByKeyAndWindow(_ + _, windowDuration)
      .map{case (topic, count) => (topic, count)}
      .transform(_.sortBy(pair => pair._2, ascending = false))


  /**
   * App config:
   *
   * Don't put your Twitter credentials in the config file (app.properties) shipped with the packaged app.
   * Instead, place the app.properties containing the Twitter API keys somewhere in your filesystem
   * and access it by using the TWEETALYZER_CONF_DIR environment variable.
   *
   * @return Application properties
   */
  private def loadConf(): Properties = {

    val conf = new Properties()

    // Resolve path to the config file
    def confMap(dir:String): String = Paths.get(dir, "app.properties").toString

    // Read the config from the filesystem, or fall back to the bundled one (not recommended, see above)
    System.getenv("TWEETALYZER_CONF_DIR") match {
      case null => conf.load(getClass.getResourceAsStream(confMap("/")))
      case foundPath => conf.load(Source.fromFile(confMap(foundPath)).bufferedReader())
    }

    // Load Twitter credentials
    loadTwitterKeys(conf)

    conf
  }

  private def loadTwitterKeys(conf:Properties): Unit = {
    val (consumerKey, consumerSecret, accessToken, accessTokenSecret) =
      ( conf.getProperty("consumer.key"),
        conf.getProperty("consumer.secret"),
        conf.getProperty("access.token"),
        conf.getProperty("access.token.secret"))

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generate OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)
  }

  def main(args: Array[String]) {

    val conf = loadConf()

    // Timing and frequency
    def getSeconds(propName:String): Duration = Seconds(conf.getProperty(propName).toLong)

    val (streamSeconds, twitterTagSeconds, twitterTagHistorySeconds) =
      ( getSeconds("stream.seconds"),
        getSeconds("twitter.tag.seconds"),
        getSeconds("twitter.tag.history.seconds"))

    // Spark config
    val masterUrl = conf.getProperty("master.url", "local[2]")
    val appName = getClass.getSimpleName
    val checkpointDir = Files.createTempDirectory(appName).toString
    val sparkConf = new SparkConf().setMaster(masterUrl).setAppName(appName)

    val ssc = new StreamingContext(sparkConf, streamSeconds)
    ssc.checkpoint(checkpointDir)

    val stream = TwitterUtils.createStream(ssc, None).map(status => status.getText)

    // Count the most popular hashtags
    val topCounts = count(stream, twitterTagSeconds)
    val topCountsHistory = count(stream, twitterTagHistorySeconds)

    // Output directory
    val outputDir = conf.getProperty("output.dir", "tweets")

    // Prints Top N topics both to the console and the output directory
    val topN = 10

    def printTop10(counts:DStream[(String, Int)]) =
      counts.foreachRDD(rdd => {
        val topList = rdd.take(topN)
        logger.info(s"\nPopular topics in last $twitterTagHistorySeconds seconds (%s total):".format(rdd.count()))
        topList.foreach{case (count, tag) => logger.info("%s (%s tweets)".format(tag, count))}
        rdd.saveAsTextFile(outputDir + "_" + Platform.currentTime)
      })

    printTop10(topCounts)
    printTop10(topCountsHistory)

    ssc.start()
    ssc.awaitTermination()
  }

}
