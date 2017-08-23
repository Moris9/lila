package lila.mod

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class UserSearch(
    securityApi: lila.security.SecurityApi,
    emailValidator: lila.security.EmailAddressValidator
) {

  def apply(query: String): Fu[List[User]] =
    if (query.isEmpty) fuccess(Nil)
    else EmailAddress.from(query).map(searchEmail) orElse
      IpAddress.from(query).map(searchIp) getOrElse
      searchUsername(query)

  private def searchIp(ip: IpAddress) =
    securityApi recentUserIdsByIp ip map (_.reverse) flatMap UserRepo.usersFromSecondary

  private def searchUsername(username: String) = UserRepo named username map (_.toList)

  private def searchEmail(email: EmailAddress): Fu[List[User]] =
    emailValidator.validate(email) ?? { fixed =>
      UserRepo.byEmail(fixed) flatMap { current =>
        UserRepo.byPrevEmail(fixed) map current.toList.:::
      }
    }
}
