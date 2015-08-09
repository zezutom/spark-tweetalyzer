package org.zezutom.spark.tweetalyzer

import java.nio.file.{Files, Paths}
import java.util.Properties

import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import twitter4j.Status
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder

import scala.io.Source

class Util(val filename: String = "app.properties", val confDir: String = "TWEETALYZER_CONF_DIR") {

  /**
   * Application config:
   *
   * Don't put your Twitter credentials in the config file (app.properties) shipped with the packaged app.
   * Instead, place the app.properties containing the Twitter API keys somewhere in your filesystem
   * and access it by using the TWEETALYZER_CONF_DIR environment variable.
   */
  val conf = new Properties()

  // Read the config from the filesystem, or fall back to the bundled one (not recommended, see above)
  System.getenv(confDir) match {
    case path if path != null && Files.exists(Paths.get(path)) =>
      conf.load {Source.fromFile(confMap(path)).bufferedReader()}
    case _ =>
      conf.load {getClass.getResourceAsStream(confMap("/").toString)}
  }

  def streamContext(appName: String): StreamingContext = {
    // Spark config
    val masterUrl = conf.getProperty("master.url", "local[2]")
    val checkpointDir = Files.createTempDirectory(appName).toString
    val sparkConf = new SparkConf().setMaster(masterUrl).setAppName(appName)

    val ssc = new StreamingContext(sparkConf, getSeconds("stream.seconds"))
    ssc.checkpoint(checkpointDir)

    ssc
  }

  def stream(ssc: StreamingContext, debug: Boolean = true): ReceiverInputDStream[Status] = {
    val oauthConf = new ConfigurationBuilder()
                      .setDebugEnabled(debug)
                      .setOAuthConsumerKey(conf.getProperty("consumer.key"))
                      .setOAuthConsumerSecret(conf.getProperty("consumer.secret"))
                      .setOAuthAccessToken(conf.getProperty("access.token"))
                      .setOAuthAccessTokenSecret(conf.getProperty("access.token.secret"))
                      .build()
    val auth = new OAuthAuthorization(oauthConf)
    TwitterUtils.createStream(ssc, Some(auth))
  }

  // Timing and frequency
  def getSeconds(propName:String): Duration = Seconds(conf.getProperty(propName).toLong)

  // Helper methods

  private def confMap(dir: String): String = Paths.get(dir, filename).toString
}

object Util {
  val instance = new Util()
}