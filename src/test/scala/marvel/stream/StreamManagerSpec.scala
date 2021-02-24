package marvel.stream

import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.scalatest.{FunSpec, Matchers}

import java.io.File

class StreamManagerSpec extends FunSpec with Matchers with StrictLogging {

  private def fixture = new {
    // warehouseLocation points to the default location for managed databases and tables
    val warehouseLocation = new File("/opt/hive/warehouse").getAbsolutePath
    System.setProperty("hadoop.home.dir", "C:\\hadoop")
    val sparkSession: SparkSession = StreamManager.createLocalSparkSession()
  }

  describe("create a parquet file") {
    val f = fixture
    implicit val sparkSession = f.sparkSession
    StreamManager.normaliseData("src/test/resources/marvelData/", "project/target/output")
    val outputDF = sparkSession.read.parquet("project/target/output")
    it("should be the correct size") {
      outputDF.count shouldBe 5
    }
    it("should have 2 character IDs") {
      val characterIds = outputDF.select(col("characterID")).distinct()
        characterIds.count shouldBe 3
      characterIds.collect().map(_.toSeq).flatten should contain allOf (1010740, 1009220, null)
    }
  }

}
