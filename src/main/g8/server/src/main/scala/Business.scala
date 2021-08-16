import Model._
import zio._
import zio.stream._

trait Business {
  def getAllUsers(): ZIO[Any, Throwable, List[UserEntity]]
  def getUser(id: UserId): ZIO[Any, Throwable, UserEntity]
  def removeUser(id: UserId): ZIO[Any, Throwable, UserEntity]
  def addUser(user: User): ZIO[Any, Throwable, UserEntity]
  def userStream(): ZStream[Any, Throwable, UserEvent]
}
object Business {
  def getAllUsers(): ZIO[Has[Business], Throwable, List[UserEntity]]    = ZIO.serviceWith[Business](_.getAllUsers())
  def getUser(id: UserId): ZIO[Has[Business], Throwable, UserEntity]    = ZIO.serviceWith[Business](_.getUser(id))
  def removeUser(id: UserId): ZIO[Has[Business], Throwable, UserEntity] = ZIO.serviceWith[Business](_.removeUser(id))
  def addUser(user: User): ZIO[Has[Business], Throwable, UserEntity]    = ZIO.serviceWith[Business](_.addUser(user))
  def userStream(): ZStream[Has[Business], Throwable, UserEvent]        = ZStream.service[Business].flatMap(_.userStream())
}

case class BusinessLive(state: State) extends Business {
  override def getAllUsers(): ZIO[Any, Throwable, List[UserEntity]]    = state.getAllUsers().commit
  override def getUser(id: UserId): ZIO[Any, Throwable, UserEntity]    = state.getUser(id).commit
  override def removeUser(id: UserId): ZIO[Any, Throwable, UserEntity] = state.removeUser(id).commit
  override def addUser(user: User): ZIO[Any, Throwable, UserEntity]    = state.addUser(UserEntit.make(user)).commit
  override def userStream(): ZStream[Any, Throwable, UserEvent]        = state.userStream
}

object BusinessLive {
  val layer: URLayer[Has[State], Has[Business]] = ZIO.service[State].map(BusinessLive(_)).toLayer
}
