package com.marvel

import com.typesafe.scalalogging.StrictLogging
import marvel.stream.StreamManager

object MarvelApp extends App with StrictLogging {

  System.setProperty("hadoop.home.dir", "C:\\hadoop")

  logger.info("Creating Spark Session")
  implicit val spark = StreamManager.createSparkSession()
  logger.info("Spark Session Created")

  val inputLocation = "hdfs://namenode:8020/project/input"
  val outputLocation = "hdfs://namenode:8020/project/output"

  logger.info("Starting Normalisation process")
  StreamManager.normaliseData(inputLocation, outputLocation)
  logger.info("Nromalised data and created Parquet file")

  spark.close()
}
