import org.apache.spark.sql.SparkSession
import stream.StreamManager

object MarvelApp extends App {

  System.setProperty("hadoop.home.dir", "C:\\hadoop")
  val spark = SparkSession.builder.master("local").appName("FlightData").getOrCreate()

  StreamManager.normaliseData(spark)
}
