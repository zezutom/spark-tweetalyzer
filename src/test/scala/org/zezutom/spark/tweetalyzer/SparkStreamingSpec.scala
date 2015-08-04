package org.zezutom.spark.tweetalyzer

import java.nio.file.Files

import org.apache.spark.ClockWrapper
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.scalatest.Suite

trait SparkStreamingSpec extends SparkSpec {
  this: Suite =>

  val batchDuration = Seconds(1)
  val checkpointDir = Files.createTempDirectory(getClass.getSimpleName).toString

  private var _ssc: StreamingContext = _
  def ssc = _ssc

  private var _clock: ClockWrapper = _
  def clock = _clock

  conf.set("spark.streaming.clock", "org.apache.spark.streaming.util.ManualClock")

  override def beforeAll(): Unit = {
    super.beforeAll()
    _ssc = new StreamingContext(sc, batchDuration)
    _ssc.checkpoint(checkpointDir)
    _clock = new ClockWrapper(ssc)

  }

  override def afterAll(): Unit = {
    if (_ssc != null) {
      // Spark gets stuck when shutting down gracefully :(
      _ssc.stop(stopSparkContext = false, stopGracefully = false)
      _ssc = null
    }
    super.afterAll()
  }
}
