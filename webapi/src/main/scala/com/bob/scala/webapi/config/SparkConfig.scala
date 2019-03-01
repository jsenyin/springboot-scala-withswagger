package com.bob.scala.webapi.config

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SparkSession}

/**
  * @Description:
  *
  * <p></p>
  * @author jsen.yin [jsen.yin@gmail.com]
  *         2019-03-01
  */
object SparkConfig {

  def getConfig(app: String): SparkConf = {
    //    import spark.implicits._
    val sparkConf: SparkConf = new SparkConf().setAppName(app).setMaster(("local[" + (System.nanoTime % 8) + "]"))
    sparkConf
  }

  def getSession(app: String): SparkSession = {

    val spark: SparkSession = SparkSession.builder
      .appName(app)
      .master(("local[" + (System.nanoTime % 8) + "]"))
      .getOrCreate()

    spark
  }

  def getContext(app: String): SQLContext = {
    val conf = new SparkConf().setMaster(("local[" + (System.nanoTime % 8) + "]")).setAppName(app)
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    sqlContext
  }

}
