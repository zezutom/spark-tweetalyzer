package org.zezutom.spark.tweetalyzer

import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}


class UtilSpec extends FlatSpec with GivenWhenThen with Matchers {

  "Application" should "be configured" in {

    Given("Util is instantiated")
    val util = new Util(confDir = "NONEXISTANT")
    val conf = util.conf
    conf should not be null

    When("Twitter credentials are obtained")
    val (consumerKey, consumerSecret, accessToken, accessTokenSecret) = (
      conf.getProperty("consumer.key"),
      conf.getProperty("consumer.secret"),
      conf.getProperty("access.token"),
      conf.getProperty("access.token.secret")
    )

    Then("Twitter credentials should be as expected")
    consumerKey should equal("TEST CONSUMER KEY")
    consumerSecret should equal("TEST SECRET KEY")
    accessToken should equal("TEST ACCESS TOKEN KEY")
    accessTokenSecret should equal("TEST ACCESS TOKEN SECRET")

    And("the credentials should be accessible in the system")
    System.getProperty("twitter4j.oauth.consumerKey") should equal(consumerKey)
    System.getProperty("twitter4j.oauth.consumerSecret") should equal(consumerSecret)
    System.getProperty("twitter4j.oauth.accessToken") should equal(accessToken)
    System.getProperty("twitter4j.oauth.accessTokenSecret") should equal(accessTokenSecret)
  }
}
