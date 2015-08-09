package org.zezutom.spark.tweetalyzer

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, GivenWhenThen, Matchers}


class UtilSpec extends FlatSpec with GivenWhenThen with Matchers with BeforeAndAfterAll {

  private var util:Util = _

  private var ssc: StreamingContext = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    util = new Util(confDir = ".")
    util.conf should not be null
  }

  override def afterAll(): Unit = {

    if (ssc != null) {
      // Spark gets stuck when shutting down gracefully :(
      ssc.stop(stopSparkContext = true, stopGracefully = false)
      ssc = null
    }
    super.afterAll()
  }

  "Twitter credentials" should "be available" in {

    Given("Twitter authentication keys")
    val authKeys = Array("consumer.key", "consumer.secret", "access.token", "access.token.secret")

    When("Twitter credentials are obtained")
    val auth = authKeys.map(key => util.conf.getProperty(key))

    Then("the credentials should be as expected")
    auth.length should equal(4)
    auth(0) should equal("TEST CONSUMER KEY")
    auth(1) should equal("TEST SECRET KEY")
    auth(2) should equal("TEST ACCESS TOKEN KEY")
    auth(3) should equal("TEST ACCESS TOKEN SECRET")
  }

  "Time intervals" should "be available" in {

    Given("polling interval keys")
    val freqKeys = Array("stream.seconds", "twitter.tag.seconds", "twitter.tag.history.seconds")

    When("durations are obtained")
    val durations = freqKeys.map(key => util.getSeconds(key))

    Then("the timing should be as expected")
    durations.length should equal(3)
    durations(0) equals Seconds(2)
    durations(1) equals Seconds(10)
    durations(2) equals Seconds(60)
  }

  "Streaming context" should "be created in sync with app config" in {

    Given("app name")
    val appName = "Test App"

    And("master url")
    val masterUrl = util.conf.getProperty("master.url")

    When("streaming context is created")
    ssc = util.streamContext(appName)

    Then("the streaming context is valid")
    ssc should not be null

    And("Spark context should be available")
    val sc = ssc.sparkContext
    sc should not be null

    And("app name is as expected")
    sc.appName equals appName

    And("master url is as expected")
    sc.master equals masterUrl
  }
}
