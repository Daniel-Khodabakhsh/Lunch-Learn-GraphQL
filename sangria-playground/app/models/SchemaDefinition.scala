package models

import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._

import scala.concurrent.Future

/**
 * Defines a GraphQL schema for the current project
 */
object SchemaDefinition {
  val characterRepo = new CharacterRepo
  /**
    * Resolves the lists of characters. These resolutions are batched and
    * cached for the duration of a query.
    */
  val characters = Fetcher.caching(
    (ctx: Unit, ids: Seq[String]) =>
      Future.successful(ids.flatMap(id => characterRepo.getHuman(id) orElse characterRepo.getDroid(id))))(HasId(_.id))

  val EpisodeEnum = EnumType(
    "Episode",
    Some("One of the films in the Star Wars Trilogy"),
    List(
      EnumValue("NEWHOPE",
        value = Episode.NEWHOPE,
        description = Some("Released in 1977.")),
      EnumValue("EMPIRE",
        value = Episode.EMPIRE,
        description = Some("Released in 1980.")),
      EnumValue("JEDI",
        value = Episode.JEDI,
        description = Some("Released in 1983."))))

  val Character: InterfaceType[Unit, Character] =
    InterfaceType(
      "Character",
      "A character in the Star Wars Trilogy",
      () => fields[Unit, Character](
        Field("id", StringType,
          Some("The id of the character."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the character."),
          resolve = _.value.name),
        Field("friends", ListType(Character),
          Some("The friends of the character, or an empty list if they have none."),
          complexity = Some((_, _, children) => 100 + 1.5 * children),
          resolve = ctx => characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e => Some(e)))
      ))

  val Human =
    ObjectType(
      "Human",
      "A humanoid creature in the Star Wars universe.",
      interfaces[Unit, Human](Character),
      fields[Unit, Human](
        Field("id", StringType,
          Some("The id of the human."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the human."),
          resolve = _.value.name),
        Field("friends", ListType(Character),
          Some("The friends of the human, or an empty list if they have none."),
          complexity = Some((_, _, children) => 100 + 1.5 * children),
          resolve = ctx => characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e => Some(e))),
        Field("homePlanet", OptionType(StringType),
          Some("The home planet of the human, or null if unknown."),
          resolve = _.value.homePlanet)
      ))

  val Droid = ObjectType(
    "Droid",
    "A mechanical creature in the Star Wars universe.",
    interfaces[Unit, Droid](Character),
    fields[Unit, Droid](
      Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id
      ),
      Field("name", OptionType(StringType),
        Some("The name of the droid."),
        resolve = ctx => Future.successful(ctx.value.name)
      ),
      Field("friends", ListType(Character),
        Some("The friends of the droid, or an empty list if they have none."),
        complexity = Some((_, _, children) => 100 + 1.5 * children),
        resolve = ctx => characters.deferSeqOpt(ctx.value.friends)
      ),
      Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
        Some("Which movies they appear in."),
        resolve = _.value.appearsIn map (e => Some(e))
      ),
      Field("primaryFunction", OptionType(StringType),
        Some("The primary function of the droid."),
        resolve = _.value.primaryFunction
      )
    ))

  val ID = Argument("id", StringType, description = "id of the character")

  val EpisodeArg = Argument("episode", OptionInputType(EpisodeEnum),
    description = "If omitted, returns the hero of the whole saga. If provided, returns the hero of that particular episode.")

  val optionalId = Argument("id", OptionInputType(StringType), description = "id of the character")

  val Query = ObjectType(
    "Query", fields[Unit, Unit](
      Field(
        "hero",
        Character,
        arguments = List(EpisodeArg),
        resolve = (ctx) => CharacterRepo.droids.last
      ),
      Field(
        "human",
        OptionType(Human),
        arguments = List(ID),
        resolve = ctx => characterRepo.getHuman(ctx.arg(ID))
      ),
      Field(
        "humans",
        ListType(OptionType(Human)),
        arguments = List(optionalId),
        resolve = ctx =>
          if (ctx.argOpt(optionalId) == None) CharacterRepo.humans.map(Some(_)) else List(characterRepo.getHuman(ctx.arg(optionalId).get))
      ),
      Field(
        "droid",
        Droid,
        arguments = List(ID),
        resolve = Projector((ctx, f) => characterRepo.getDroid(ctx.arg(ID)).get)
      )
    )
  )

  val StarWarsSchema = Schema(Query)
}
