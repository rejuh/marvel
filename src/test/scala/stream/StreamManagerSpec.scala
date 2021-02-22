package stream

import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.sql.SparkSession
import org.scalatest.{FunSpec, Matchers}

class StreamManagerSpec extends FunSpec with Matchers with StrictLogging {

  private def fixture = new {
    System.setProperty("hadoop.home.dir", "C:\\hadoop")
    val sparkSession: SparkSession = SparkSession
      .builder
      .master("local")
      .appName("Marvel")
      .getOrCreate()
  }

  describe("test creating parquet") {
    it("create parquet") {
      val f = fixture
      StreamManager.normaliseData(f.sparkSession)
    }
  }

}