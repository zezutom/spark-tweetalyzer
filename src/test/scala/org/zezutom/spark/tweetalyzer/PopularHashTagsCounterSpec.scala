package org.zezutom.spark.tweetalyzer

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, Time}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class PopularHashTagsCounterSpec  extends FlatSpec with SparkStreamingSpec with GivenWhenThen with Matchers with Eventually with IntegrationPatience {

  private val windowDuration = Seconds(4)

  "Sample set" should "be counted" in {
    Given("streaming context is initialized")
    val lines = mutable.Queue[RDD[String]]()
    var result = ListBuffer.empty[Array[(String, Int)]]

    PopularHashTagsCounter.count(ssc.queueStream(lines), windowDuration)
      .foreachRDD((rdd, time: Time) => result += rdd.collect())

    ssc.start()

    When("first set of tweets queued")
    lines += sc.makeRDD(Seq("Lorem ipsum #a dolor #b sit amet #b"))

    Then("hashtags counted after the first slide")
    clock.advance(windowDuration.milliseconds)
    eventually {
      result.last should equal(Array(
        ("#b", 2),
        ("#a", 1)
      ))
    }

    When("second set of tweets queued")
    lines += sc.makeRDD(Seq("consectetur adipiscing #a elit, sed do #b"))

    Then("hashtags counted after the second slide")
    clock.advance(windowDuration.milliseconds)
    eventually {
      result.last should equal(Array(
        ("#b", 3),
        ("#a", 2)
      ))
    }

    When("nothing more queued")

    Then("hashtags counted after third slide")
    eventually {
      result.last should equal(Array())
    }

  }
}
