import zio.magic._
import zio._
import zio.console._
import zio.stream.ZStream

object Main extends zio.App {
  val port = 8080

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program
      .inject(ZEnv.live, StateLive.layer, BusinessLive.layer)
      .exitCode

  lazy val program: RIO[ZEnv with Api.Deps, Unit] = {
    for {
      _ <- putStrLn(s"starting server on port \${port}")
      s <- startServer().forkDaemon
      _ <- putStrLn(s"server started. Hit Enter to exit")
      _ <- getStrLn
      _ <- (putStrLn("shutting down ") *> s.interrupt)
    } yield ()
  }

  def startServer(): RIO[ZEnv with Api.Deps, Unit] = {
    import caliban.ZHttpAdapter
    import caliban.GraphQL
    import zhttp.http._
    import zhttp.service.Server

    val graphiql = Http.succeed(Response.http(content = HttpData.fromStream(ZStream.fromResource("graphiql.html"))))

    val graphQl: GraphQL[ZEnv with Api.Deps] = Api.graphQl

    for {
      interpreter <- graphQl.interpreter
      _ <- Server
             .start(
               port,
               Http.route {
                 case _ -> Root / "api" / "graphql" => ZHttpAdapter.makeHttpService(interpreter)
                 case _ -> Root / "ws" / "graphql"  => ZHttpAdapter.makeWebSocketService(interpreter)
                 case _ -> Root / "graphiql"        => graphiql
               }
             )
             .forever
    } yield ()
  }
}
