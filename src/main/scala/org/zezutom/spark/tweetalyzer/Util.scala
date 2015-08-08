package org.zezutom.spark.tweetalyzer

import java.nio.file.{Files, Paths}
import java.util.Properties

import scala.io.Source

class Util(val filename: String = "app.properties", val confDir: String = "TWEETALYZER_CONF_DIR") {

  private def confMap(dir: String): String = Paths.get(dir, filename).toString

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

  // Set the system properties so that Twitter4j library used by twitter stream
  // can use them to generate OAuth credentials
  System.setProperty("twitter4j.oauth.consumerKey", conf.getProperty("consumer.key"))
  System.setProperty("twitter4j.oauth.consumerSecret", conf.getProperty("consumer.secret"))
  System.setProperty("twitter4j.oauth.accessToken", conf.getProperty("access.token"))
  System.setProperty("twitter4j.oauth.accessTokenSecret", conf.getProperty("access.token.secret"))
}

object Util {
  val instance = new Util()
}