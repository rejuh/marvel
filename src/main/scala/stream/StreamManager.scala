package stream

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions.broadcast
import schema.{CharacterStats, Characters, CharactersToComics, Comics, MarvelCharactersInfo, MarvelDcCharacters, SuperHeroesPowerMatrix}

object StreamManager extends StrictLogging {
  //temp string references to files
  val characters = "src/main/resources/marvelData/characters.csv"
  val characterToComics = "src/main/resources/marvelData/charactersToComics.csv"
  val characterStats = "src/main/resources/marvelData/charcters_stats.csv"
  val comics = "src/main/resources/marvelData/comics.csv"
  val marvelCharactersInfo = "src/main/resources/marvelData/marvel_characters_info.csv"
  val marvelDcCharacters = "src/main/resources/marvelData/marvel_dc_characters.csv"
  val superHeroPowerMatrix = "src/main/resources/marvelData/superheroes_power_matrix.csv"

  def normaliseData(sparkSession: SparkSession): Unit = {
    import sparkSession.implicits._
    import org.apache.spark.sql.Encoders
    val charactersSchema = Encoders.product[Characters].schema
    val characterToComicSchema = Encoders.product[CharactersToComics].schema
    val characterStatsSchema = Encoders.product[CharacterStats].schema
    val comicsSchema = Encoders.product[Comics].schema
    val marvelCharactersInfoSchema = Encoders.product[MarvelCharactersInfo].schema
    val marvelDcCharactersSchema = Encoders.product[MarvelDcCharacters].schema
    val superHeroPowerMatrixSchema = Encoders.product[SuperHeroesPowerMatrix].schema

    val characterData = sparkSession.read.option("header", "true").schema(charactersSchema).csv(characters).as[Characters]
    val characterToComicData = sparkSession.read.option("header", "true").schema(characterToComicSchema).csv(characterToComics).as[CharactersToComics]
    val characterStatData = sparkSession.read.option("header", "true").schema(characterStatsSchema).csv(characterStats).as[CharacterStats]
    val comicsData = sparkSession.read.option("header", "true").schema(comicsSchema).csv(comics).as[Comics]
    val marvelCharactersInfoData = sparkSession.read.option("header", "true").schema(marvelCharactersInfoSchema).csv(marvelCharactersInfo).as[MarvelCharactersInfo]
    val marvelDcCharactersData = sparkSession.read.option("header", "true").schema(marvelDcCharactersSchema).csv(marvelDcCharacters).as[MarvelDcCharacters]
    val superHeroPowerMatrixData = sparkSession.read.option("header", "true").schema(superHeroPowerMatrixSchema).csv(superHeroPowerMatrix).as[SuperHeroesPowerMatrix]

    val out = characterToComicData.joinWith(characterData, characterData("characterID") === characterToComicData("characterID"), "inner")
      .joinWith(comicsData, $"_1.comicID" === comicsData("comicID"), "inner")

    out.count()

    out.write.mode(SaveMode.Overwrite).parquet("target/output/output.parquet")

  }

}
