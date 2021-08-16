import caliban.GraphQL
import caliban.GraphQL.graphQL
import caliban.RootResolver
import caliban.schema.GenericSchema
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers._
import zio.duration.durationInt
import zio.stream.ZStream
import zio._

import scala.language.postfixOps

object Api {
  type Deps = Has[Business]

  object schema extends GenericSchema[ZEnv with Deps]
  import schema._

  val graphQl: GraphQL[ZEnv with Deps] =
    graphQL(
      RootResolver(
        Queries(
          getUser = args => Business.getUser(args.userId),
          getAllUsers = Business.getAllUsers()
        ),
        Mutations(
          addUser = args => Business.addUser(args.user),
          removeUser = args => Business.removeUser(args.userId)
        ),
        Subscriptions(
          userEvents = Business.userStream()
        )
      )
    ) @@
      maxFields(200) @@               // query analyzer that limit query fields
      maxDepth(30) @@                 // query analyzer that limit query depth
      timeout(3 seconds) @@           // wrapper that fails slow queries
      printSlowQueries(500 millis) @@ // wrapper that logs slow queries
      printErrors @@                  // wrapper that logs errors
      apolloTracing                   // wrapper for https://github.com/apollographql/apollo-tracing

  case class GetUserArgs(userId: Model.UserId)
  case class AddUserArgs(user: Model.User)
  case class RemoveUserArgs(userId: Model.UserId)

  case class Queries(
    getUser: GetUserArgs => ZIO[Deps, Throwable, Model.UserEntity],
    getAllUsers: ZIO[Deps, Throwable, List[Model.UserEntity]]
  )

  case class Mutations(
    addUser: AddUserArgs => ZIO[Deps, Throwable, Model.UserEntity],
    removeUser: RemoveUserArgs => ZIO[Deps, Throwable, Model.UserEntity]
  )

  case class Subscriptions(userEvents: ZStream[Deps, Throwable, Model.UserEvent])
}
