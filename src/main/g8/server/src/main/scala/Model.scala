import java.util.UUID

object Model {
  case class UserId(value: String) extends AnyVal
  case class User(name: String, age: Int)
  case class UserEntity(id: UserId, user: User)

  object UserEntit {
    def make(user: User) = UserEntity(UserId(UUID.randomUUID().toString()), user)
  }

  sealed trait UserEvent
  object UserEvent {
    case class Added(userEntity: UserEntity)   extends UserEvent
    case class Removed(userEntity: UserEntity) extends UserEvent
  }
}
