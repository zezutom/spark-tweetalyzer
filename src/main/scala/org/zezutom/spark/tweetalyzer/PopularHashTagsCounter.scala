package org.zezutom.spark.tweetalyzer

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.dstream.DStream
import scala.compat.Platform

/**
 * $SPARK_HOME/bin/spark-submit \
 * --class "org.zezutom.spark.tweetalyzer.PopularHashTagsCounter" \
 * target/scala-2.11/tweetalyzer-assembly-0.1.0.jar
 */
object PopularHashTagsCounter extends LazyLogging {

  val util = Util.instance

  // Transforms a stream into a hash count map
  def count(stream:DStream[String], windowDuration: Duration): DStream[(String, Int)] =
    stream
      .flatMap(text => text.split(" ").filter(_.startsWith("#")))   // extract hashtags
      .map((_, 1)).reduceByKeyAndWindow(_ + _, windowDuration)
      .map{case (topic, count) => (topic, count)}
      .transform(_.sortBy(pair => pair._2, ascending = false))

  def main(args: Array[String]) {

    val ssc = util.streamContext(getClass.getSimpleName)

    val stream = util.stream(ssc).map(status => status.getText)

    val twitterTagHistorySeconds: Duration = util.getSeconds("twitter.tag.history.seconds")

    // Count the most popular hashtags
    val topCounts = count(stream, util.getSeconds("twitter.tag.seconds"))
    val topCountsHistory = count(stream, twitterTagHistorySeconds)

    // Output directory
    val outputDir = util.conf.getProperty("output.dir", "tweets")

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
