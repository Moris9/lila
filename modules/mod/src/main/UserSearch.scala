package lila.mod

import lila.user.{ User, UserRepo }

final class UserSearch(securityApi: lila.security.Api) {

  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipPattern = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r.pattern

  // from playframework
  private val emailPattern =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r.pattern

  def apply(query: String): Fu[List[User]] =
    if (query.isEmpty) fuccess(Nil)
    else if (ipPattern.matcher(query).matches) searchIp(query)
    else if (emailPattern.matcher(query).matches) searchEmail(query)
    else searchUsername(query)

  private def searchIp(ip: String) =
    securityApi recentUserIdsByIp ip flatMap UserRepo.byOrderedIds

  private def searchUsername(username: String) = UserRepo named username map (_.toList)

  private def searchEmail(email: String) = UserRepo byEmail email map (_.toList)
}
