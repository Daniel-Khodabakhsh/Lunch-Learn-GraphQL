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

  val flowNodeType = ObjectType(
    name = "FlowNode",
    description = "Node within a flow.",
    fields = fields[Unit, FlowNode](
      Field(
        name = "id",
        fieldType = IntType,
        description = Some("The identifier of the flow node."),
        resolve = _.value.id
      ),
      Field(
        name = "name",
        fieldType = StringType,
        description = Some("Flow node's name."),
        resolve = _.value.name
      ),
      Field(
        name = "nodeType",
        fieldType = StringType,
        description = Some("Node's type."),
        resolve = _.value.nodeType
      ),
      Field(
        name = "connectedTo",
        fieldType = ListType(IntType),
        description = Some("Nodes connected to by this node."),
        resolve = _.value.connectedTo.map(connection => connection.id)
      )
    )
  )

  val flowType = ObjectType(
    name = "Flow",
    description = "Flow including flow nodes.",
    fields = fields[Unit, Flow](
      Field(
        name = "id",
        fieldType = IntType,
        description = Some("The identifier of the flow."),
        resolve = _.value.id
      ),
      Field(
        name = "name",
        fieldType = StringType,
        description = Some("Flow's name."),
        resolve = _.value.name
      ),
      Field(
        name = "nodes",
        fieldType = ListType(flowNodeType),
        description = Some("Flow's nodes."),
        resolve = _.value.nodes
      )
    )
  )

  val userType = ObjectType(
    name = "User",
    description = "User of the system.",
    fields = fields[Unit, User](
      Field(
        name = "id",
        fieldType = IntType,
        description = Some("The identifier of the user."),
        resolve = _.value.id
      ),
      Field(
        name = "name",
        fieldType = StringType,
        description = Some("Username."),
        resolve = _.value.name
      ),
      Field(
        name = "localisation",
        fieldType = StringType,
        description = Some("User's localisation."),
        resolve = _.value.localisation
      ),
    )
  )

  val pageType = ObjectType(
    name = "Page",
    description = "List of pages on the system.",
    fields = fields[Unit, Page](
      Field(
        name = "id",
        fieldType = IntType,
        description = Some("The identifier of the page."),
        resolve = _.value.id
      ),
      Field(
        name = "flows",
        fieldType = ListType(flowType),
        description = Some("The identifier of the page."),
        resolve = _.value.flows
      ),
      Field(
        name = "owner",
        fieldType = userType,
        description = Some("The owner of the page."),
        resolve = _.value.owner
      )
    )
  )

  val idParameterName = "id"

  val characterId = Argument(idParameterName, StringType, description = "id of the character")

  val queryType = ObjectType(
    "Query", fields[Unit, Unit](
      Field(
        "hero",
        Character,
        arguments = List(
          Argument(
            name = "episode",
            argumentType = OptionInputType(EpisodeEnum),
            description =  "If omitted, returns the hero of the whole saga. If provided, returns the hero of that particular episode."
          )
        ),
        resolve = (ctx) => CharacterRepo.droids.last
      ),
      Field(
        "human",
        OptionType(Human),
        arguments = List(
          characterId
        ),
        resolve = ctx => characterRepo.getHuman(ctx.arg(idParameterName))
      ),
      Field(
        "humans",
        ListType(OptionType(Human)),
        arguments = List(
          Argument(idParameterName, OptionInputType(StringType), description = s"${idParameterName} of the character")
        ),
        resolve = ctx => {
          if (ctx.argOpt(idParameterName) == None)
            CharacterRepo.humans.map(Some(_))
          else
            List(characterRepo.getHuman(ctx.arg(idParameterName)))
        }
      ),
      Field(
        "droid",
        Droid,
        arguments = List(
          characterId
        ),
        resolve = Projector((ctx, f) => characterRepo.getDroid(ctx.arg(idParameterName)).get)
      ),
      Field(
        name = "pages",
        fieldType = ListType(pageType),
        resolve = ctx => MoreData.pages
      ),
      Field(
        name = "flows",
        fieldType = ListType(OptionType(flowType)),
        arguments = List(
          Argument(idParameterName, OptionInputType(IntType), description = s"${idParameterName} of the flow")
        ),
        resolve = ctx => {
          if (ctx.argOpt(idParameterName) == None)
            MoreData.flows.map(Some(_))
          else
            List(MoreData.flows.find(f => f.id == ctx.argOpt[Int](idParameterName).get))
        }
      ),
      Field(
        name = "currentUser",
        fieldType = OptionType(userType),
        resolve = ctx => MoreData.users.last
      )
    )
  )

  val StarWarsSchema = Schema(queryType)
}
