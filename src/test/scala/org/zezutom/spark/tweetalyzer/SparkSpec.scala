package org.zezutom.spark.tweetalyzer

import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait SparkSpec extends BeforeAndAfterAll {
  this: Suite =>

  private val master = "local[2]"
  private val appName = getClass.getSimpleName

  private var _sc: SparkContext = _
  def sc = _sc

  val conf: SparkConf = new SparkConf()
    .setMaster(master)
    //.set("spark.driver.host", "localhost")
    .setAppName(appName)

  override def beforeAll(): Unit = {
    super.beforeAll()
    _sc = new SparkContext(conf)
  }

  override def afterAll(): Unit = {
    if (_sc != null) {
      _sc.stop()
      _sc = null
    }
    super.afterAll()
  }
}
