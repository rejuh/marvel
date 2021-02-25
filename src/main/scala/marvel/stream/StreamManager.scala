package marvel.stream

import com.marvel.schema.{CharacterStats, Characters, CharactersToComics, Comics, MarvelCharactersInfo, MarvelDcCharacters, SuperHeroesPowerMatrix}
import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.sql.functions.{broadcast, col}
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}

import java.io.File

object StreamManager extends StrictLogging {
  //temp string references to files
  val characters = "characters.csv"
  val characterToComics = "charactersToComics.csv"
  val characterStats = "charcters_stats.csv"
  val comics = "comics.csv"
  val marvelCharactersInfo = "marvel_characters_info.csv"
  val marvelDcCharacters = "marvel_dc_characters.csv"
  val superHeroPowerMatrix = "superheroes_power_matrix.csv"

  def normaliseData(inputLocation: String, outputLocation: String)(implicit sparkSession: SparkSession): Unit = {
    import sparkSession.implicits._
    import org.apache.spark.sql.Encoders

    logger.info("Creating schemas")
    val charactersSchema = Encoders.product[Characters].schema
    val characterToComicSchema = Encoders.product[CharactersToComics].schema
    val characterStatsSchema = Encoders.product[CharacterStats].schema
    val comicsSchema = Encoders.product[Comics].schema
    val marvelCharactersInfoSchema = Encoders.product[MarvelCharactersInfo].schema
    val marvelDcCharactersSchema = Encoders.product[MarvelDcCharacters].schema
    val superHeroPowerMatrixSchema = Encoders.product[SuperHeroesPowerMatrix].schema
    logger.info("Schemas created")

    logger.info(s"reading data from specified location - ${inputLocation}")
    val characterData = sparkSession.read.option("header", "true").schema(charactersSchema).csv(inputLocation + characters).as[Characters].as("characters").withColumnRenamed("name", "Name").repartition(2, col("Name"))
    val characterToComicData = sparkSession.read.option("header", "true").schema(characterToComicSchema).csv(inputLocation + characterToComics).as[CharactersToComics].as("characterToComic")
    val characterStatData = sparkSession.read.option("header", "true").schema(characterStatsSchema).csv(inputLocation + characterStats).as[CharacterStats]
    val comicsData = sparkSession.read.option("header", "true").schema(comicsSchema).csv(inputLocation + comics).as[Comics].as("comic")
    val marvelCharactersInfoData = sparkSession.read.option("header", "true").schema(marvelCharactersInfoSchema).csv(inputLocation + marvelCharactersInfo).as[MarvelCharactersInfo].repartition(2, col("Name"))
    val marvelDcCharactersData = sparkSession.read.option("header", "true").schema(marvelDcCharactersSchema).csv(inputLocation + marvelDcCharacters).as[MarvelDcCharacters].repartition(2, col("Name"))
    val superHeroPowerMatrixData = sparkSession.read.option("header", "true").schema(superHeroPowerMatrixSchema).csv(inputLocation + superHeroPowerMatrix).as[SuperHeroesPowerMatrix]
      .withColumnRenamed("Durability", "Durable")
      .withColumnRenamed("Intelligence", "Intelligent")
      .repartition(2, col("Name"))
    logger.info("Dataframes created, performing joins")

    val joinMarvelData = mergeMarvelDcData(marvelCharactersInfoData, marvelDcCharactersData).repartition(5, col("Name"))

    val out = characterToComicData.join(broadcast(characterData), Seq("characterID"), "outer")
      .join(comicsData, Seq("comicID"), "outer")
      .join(broadcast(characterStatData), Seq("Name"), "outer")
      .join(broadcast(superHeroPowerMatrixData), Seq("Name"), "outer")
      .join(joinMarvelData, Seq("Name", "Alignment"), "outer")

    val transformedData = out.toDF(out.schema
      .fieldNames
      .map(name => "[ ,;{}()\\n\\t=\\-\\\\\\/]+".r.replaceAllIn(name, "_")): _*)
    logger.info("Data joined, generating Parquet")

    transformedData.coalesce(1).write.mode(SaveMode.Overwrite).parquet(s"${outputLocation}")
    logger.info(s"Parquet created at ${outputLocation}")
  }

  def loadToHive(parquetLocation: String)(implicit sparkSession: SparkSession): Unit = {
    val sqlContext = sparkSession.sqlContext

    logger.info("reading parquet file")
    val parquetData = sparkSession.read.parquet(s"${parquetLocation}")

    logger.info("creating temporary view")
    parquetData.createOrReplaceTempView("temporaryMarvelTable")
    logger.info("creating table in hive")
    sqlContext.sql("create table marvelTable as select * from temporaryMarvelTable")
    logger.info("loaded data into table: marvelTable")

  }

  private def mergeMarvelDcData(marvelCharactersInfoData: Dataset[MarvelCharactersInfo], marvelDcCharactersData: Dataset[MarvelDcCharacters]) = {
    marvelCharactersInfoData.join(marvelDcCharactersData, Seq("ID", "name", "eyecolor", "haircolor", "alignment", "gender"), "outer")
  }

  def createSparkSession(): SparkSession = {
    val warehouseLocation = new File("/user/hive/warehouse").getAbsolutePath
    SparkSession
      .builder
      .appName("Marvel")
      .config("fs.defaultFS", "hdfs://namenode:8020")
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .config("hive.metastore.uris", "thrift://hive-metastore:9083")
      .enableHiveSupport()
      .getOrCreate()
  }

  def createLocalSparkSession(): SparkSession = {
    SparkSession
      .builder
      .master("local")
      .appName("Marvel")
      .getOrCreate()
  }
}
