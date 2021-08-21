import zio.Has
import zio.Hub
import zio.stm._
import zio.stream._
import zio.optics.toptics._
import Model._
import zio.ULayer
import zio.ZIO

trait State {
  def getAllUsers(): ZSTM[Any, Throwable, List[UserEntity]]
  def getUser(id: UserId): ZSTM[Any, Throwable, UserEntity]
  def removeUser(id: UserId): ZSTM[Any, Throwable, UserEntity]
  def addUser(userEntity: UserEntity): ZSTM[Any, Throwable, UserEntity]
  def userStream: ZStream[Any, Throwable, UserEvent]
}

object State {
  def getAllUsers(): ZSTM[Has[State], Throwable, List[UserEntity]]             = ZSTM.serviceWith(_.getAllUsers())
  def getUser(id: UserId): ZSTM[Has[State], Throwable, UserEntity]             = ZSTM.serviceWith(_.getUser(id))
  def removeUser(id: UserId): ZSTM[Has[State], Throwable, UserEntity]          = ZSTM.serviceWith(_.removeUser(id))
  def addUser(userEntity: UserEntity): ZSTM[Has[State], Throwable, UserEntity] = ZSTM.serviceWith(_.addUser(userEntity))
  def userStream: ZStream[Has[State], Throwable, UserEvent]                    = ZStream.service[State].flatMap(_.userStream)
}

case class StateLive(
  hub: Hub[Take[Nothing, UserEvent]],
  queue: TQueue[Take[Nothing, UserEvent]],
  users: TMap[UserId, UserEntity]
) extends State {

  override def getAllUsers(): ZSTM[Any, Throwable, List[UserEntity]] = users.values

  override def getUser(id: UserId): ZSTM[Any, Throwable, UserEntity] = users.key(id).get(())

  override def removeUser(id: UserId): ZSTM[Any, Throwable, UserEntity] = for {
    userToRemove <- getUser(id)
    _            <- users.delete(id)
    _            <- queue.offer(Take.single(UserEvent.Removed(userToRemove)))
  } yield userToRemove

  override def addUser(userEntity: UserEntity): ZSTM[Any, Throwable, UserEntity] = for {
    _ <- users.put(userEntity.id, userEntity)
    _ <- queue.offer(Take.single(UserEvent.Added(userEntity)))
  } yield userEntity

  override def userStream: ZStream[Any, Throwable, UserEvent] =
    ZStream.fromHub(hub).flattenTake

  private def shutdown =
    (queue.takeAll *> queue.offer(Take.end)).commit.ensuring(hub.shutdown).uninterruptible
}

object StateLive {
  private val stateLive: ZIO[Any, Nothing, StateLive] =
    for {
      hub   <- Hub.dropping[Take[Nothing, UserEvent]](1024)
      queue <- TQueue.bounded[Take[Nothing, UserEvent]](1024).commit
      _     <- ZStream.fromTQueue(queue).flattenTake.intoHub(hub).fork
      users <- TMap.empty[UserId, UserEntity].commit
    } yield (StateLive(hub, queue, users))

  val layer: ULayer[Has[State]] = stateLive.toManaged(_.shutdown).toLayer
}
