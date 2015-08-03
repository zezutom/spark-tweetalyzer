package org.zezutom.spark.tweetalyzer

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, Time}
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class PopularHashTagsCounterSpec  extends FlatSpec with SparkStreamingSpec with GivenWhenThen with Matchers with Eventually {

  private val windowDuration = Seconds(4)

  // A default timeout for the trait 'eventually'
  // Please bear in mind the timing matters (millis) and is a bit shaky
  // Try to change the milliseconds below if there are unexplained failures in the 'eventually' block
  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(2500, Millis)))

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
