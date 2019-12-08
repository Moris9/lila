package lila

import com.github.benmanes.caffeine.cache.{ Cache => CaffeineCache }
import kamon.Kamon
import kamon.tag.TagSet

import lila.common.ApiVersion

object mon {

  private def apiTag(api: Option[ApiVersion]) = api.fold("web")(_.toString)

  object http {
    private val timeGauge = Kamon.gauge("http.time")
    def time(action: String, api: Option[ApiVersion]) = timeGauge.withTags(
      TagSet.from(Map("action" -> action, "api" -> apiTag(api)))
    ).update _
    object request {
      private val base = Kamon.counter("http.request")
      val ipv6 = base.withTag("tpe", "ipv6").increment _
      val xhr = base.withTag("tpe", "xhr").increment _
      val bot = base.withTag("tpe", "bot").increment _
      val page = base.withTag("tpe", "page").increment _
      def path(p: String) = inc("http.request.path", "path", p)
    }
    object response {
      private val codeCounter = Kamon.counter("http.response")
      def code(action: String, api: Option[ApiVersion], code: Int) = codeCounter.withTags(
        TagSet.from(Map("action" -> action, "api" -> apiTag(api), "code" -> code.toString))
      ).increment()
    }
    object prismic {
      val timeout = inc("http.prismic.timeout")
    }
    object mailgun {
      val timeout = inc("http.mailgun.timeout")
    }
    object userGames {
      def cost = incX("http.user-games.cost")
    }
    object csrf {
      private val base = Kamon.counter("http.csrf")
      def error(tpe: String, api: Option[ApiVersion]) = base.withTags(
        TagSet.from(Map("tpe" -> tpe, "api" -> apiTag(api)))
      ).increment()
    }
    object fingerPrint {
      val count = inc("http.finger_print.count")
      val time = rec("http.finger_print.time")
    }
  }
  object syncache {
    def miss(name: String) = inc("syncache.miss", "name", name)
    def wait(name: String) = inc("syncache.wait", "name", name)
    def preload(name: String) = inc("syncache.preload", "name", name)
    def timeout(name: String) = inc("syncache.timeout", "name", name)
    def waitMicros(name: String) = incX("syncache.wait_micros", "name", name)
    def computeNanos(name: String) = rec("syncache.compute_nanos", "name", name)
    def chmSize(name: String) = rec("syncache.chm.size", "name", name)
  }
  def caffeineStats(cache: CaffeineCache[_, _], name: String): Unit = {
    val stats = cache.stats
    rec("caffeine.count.hit", "name", name)(stats.hitCount)
    rate("caffeine.rate.hit", "name", name)(stats.hitRate)
    rec("caffeine.count.miss", "name", name)(stats.missCount)
    if (stats.totalLoadTime > 0) {
      rec("caffeine.count.load.success", "name", name)(stats.loadSuccessCount)
      rec("caffeine.count.load.failure", "name", name)(stats.loadFailureCount)
      rec("caffeine.total.load_time", "name", name)(stats.totalLoadTime / 1000000) // in millis; too much nanos for Kamon to handle)
      rec("caffeine.penalty.load_time", "name", name)(stats.averageLoadPenalty.toLong)
    }
    rec("caffeine.count.eviction", "name", name)(stats.evictionCount)
    rec("caffeine.count.entry", "name", name)(cache.estimatedSize)
  }
  object evalCache {
    private val hit = inc("eval_cache.all.hit")
    private val miss = inc("eval_cache.all.miss")
    private def hitIf(cond: Boolean) = if (cond) hit else miss
    private object byPly {
      def hit(ply: Int) = inc(s"eval_cache.ply.hit", "ply", ply.toString)
      def miss(ply: Int) = inc(s"eval_cache.ply.miss", "ply", ply.toString)
      def hitIf(ply: Int, cond: Boolean) = if (cond) hit(ply) else miss(ply)
    }
    def register(ply: Int, isHit: Boolean) = {
      hitIf(isHit)()
      if (ply <= 10) byPly.hitIf(ply, isHit)()
    }
    object upgrade {
      val hit = incX("eval_cache.upgrade.hit")
      val members = rec("eval_cache.upgrade.members")
      val evals = rec("eval_cache.upgrade.evals")
      val expirable = rec("eval_cache.upgrade.expirable")
    }
  }
  object lobby {
    object hook {
      val create = inc("lobby.hook.create")
      val join = inc("lobby.hook.join")
      val size = rec("lobby.hook.size")
    }
    object seek {
      val create = inc("lobby.seek.create")
      val join = inc("lobby.seek.join")
    }
    object socket {
      val getSris = rec("lobby.socket.get_uids")
      val member = rec("lobby.socket.member")
      val idle = rec("lobby.socket.idle")
      val hookSubscribers = rec("lobby.socket.hook_subscribers")
    }
    object pool {
      object wave {
        def scheduled(id: String) = inc("lobby.pool.wave.scheduled", "pool", id)
        def full(id: String) = inc("lobby.pool.wave.full", "pool", id)
        def candidates(id: String) = rec("lobby.pool.wave.candidates", "pool", id)
        def paired(id: String) = rec("lobby.pool.wave.paired", "pool", id)
        def missed(id: String) = rec("lobby.pool.wave.missed", "pool", id)
        def wait(id: String) = rec("lobby.pool.wave.wait", "pool", id)
        def ratingDiff(id: String) = rec("lobby.pool.wave.rating_diff", "pool", id)
        def withRange(id: String) = rec("lobby.pool.wave.with_range", "pool", id)
      }
      object thieve {
        def timeout(id: String) = inc("lobby.pool.thieve.timeout", "pool", id)
        def candidates(id: String) = rec("lobby.pool.thieve.candidates", "pool", id)
        def stolen(id: String) = rec("lobby.pool.thieve.stolen", "pool", id)
      }
      object join {
        def count(id: String) = inc("lobby.pool.join.count", "pool", id)
      }
      object leave {
        def count(id: String) = inc("lobby.pool.leave.count", "pool", id)
        def wait(id: String) = rec("lobby.pool.leave.wait", "pool", id)
      }
      object matchMaking {
        def duration(id: String) = rec("lobby.pool.match_making.duration", "pool", id)
      }
      object gameStart {
        def duration(id: String) = rec("lobby.pool.game_start.duration", "pool", id)
      }
    }
  }
  object rating {
    object distribution {
      def byPerfAndRating(perfKey: String, rating: Int): Rate = value =>
        Kamon.gauge("rating.distribution").withTags(
          TagSet.from(Map("perf" -> perfKey, "rating" -> rating.toString))
        ).update((value * 100000).toInt)
    }
    object regulator {
      def micropoints(perfKey: String) = rec("rating.regulator", "perf", perfKey)
    }
  }

  object round {
    object api {
      val player = rec("round.api.player")
      val watcher = rec("round.api.watcher")
      val embed = rec("round.api.embed")
    }
    object forecast {
      val create = inc("round.forecast.create")
    }
    object move {
      object lag {
        val compDeviation = rec("round.move.lag.comp_deviation")
        def uncomped(key: String) = rec("round.move.lag.uncomped_ms", "key", key)
        def uncompStdDev(key: String) = rec("round.move.lag.uncomp_stdev_ms", "key", key)
        val stdDev = rec("round.move.lag.stddev_ms")
        val mean = rec("round.move.lag.mean_ms")
        val coefVar = rec("round.move.lag.coef_var_1000")
        val compEstStdErr = rec("round.move.lag.comp_est_stderr_1000")
        val compEstOverErr = rec("round.move.lag.avg_over_error_ms")
      }
      val count = inc("round.move.count")
      val time = rec("round.move.time")
    }
    object error {
      val client = inc("round.error.client")
      val fishnet = inc("round.error.fishnet")
      val glicko = inc("round.error.glicko")
    }
    object titivate {
      val time = rec("round.titivate.time")
      val game = rec("round.titivate.game") // how many games were processed
      val total = rec("round.titivate.total") // how many games should have been processed
      val old = rec("round.titivate.old") // how many old games remain
    }
    object alarm {
      val time = rec("round.alarm.time")
      val count = rec("round.alarm.count")
    }
    object expiration {
      val count = inc("round.expiration.count")
    }
  }
  object playban {
    def outcome(out: String) = inc("playban.outcome", "outcome", out)
    object ban {
      val count = inc("playban.ban.count")
      val mins = incX("playban.ban.mins")
    }
  }
  object explorer {
    object index {
      val success = incX("explorer.index.success")
      val failure = incX("explorer.index.failure")
      val time = rec("explorer.index.time")
    }
  }
  object timeline {
    val notification = incX("timeline.notification")
  }
  object insight {
    object request {
      val count = inc("insight.request")
      val time = rec("insight.request")
    }
    object index {
      val count = inc("insight.index")
      val time = rec("insight.index")
    }
  }
  object search {
    def client(op: String) = rec("search.client", "op", op)
    def success(op: String) = inc("search.client.success", "op", op)
    def failure(op: String) = inc("search.client.failure", "op", op)
  }
  object study {
    object search {
      object index {
        def count = inc("study.search.index.count")
        def time = rec("study.search.index.time")
      }
      object query {
        def count = inc("study.search.query.count")
        def time = rec("study.search.query.time")
      }
    }
  }
  object user {
    val online = rec("user.online")
    object register {
      def count(api: Option[ApiVersion]) = inc("user.register.count", "api", apiTag(api))
      def mustConfirmEmail(v: String) = inc("user.register.must_confirm_email", "tpe", v)
      def confirmEmailResult(v: Boolean) = inc("user.register.confirm_email", "success", v.toString)
      val modConfirmEmail = inc("user.register.mod_confirm_email")
    }
    object auth {
      val bcFullMigrate = inc("user.auth.bc_full_migrate")
      val hashTime = rec("user.auth.hash_time")
      val hashTimeInc = incX("user.auth.hash_time_inc")
      def result(v: Boolean) = inc(s"user.auth.result.$v")

      def passwordResetRequest(s: String) = inc(s"user.auth.password_reset_request.$s")
      def passwordResetConfirm(s: String) = inc(s"user.auth.password_reset_confirm.$s")

      def magicLinkRequest(s: String) = inc(s"user.auth.magic_link_request.$s")
      def magicLinkConfirm(s: String) = inc(s"user.auth.magic_link_confirm.$s")
    }
    object oauth {
      object usage {
        val success = inc("user.oauth.usage.success")
        val failure = inc("user.oauth.usage.success")
      }
    }
  }
  object trouper {
    def queueSize(name: String) = rec(s"trouper.queue_size.$name")
  }
  object mod {
    object report {
      val unprocessed = rec("mod.report.unprocessed")
      val close = inc("mod.report.close")
      def create(reason: String) = inc(s"mod.report.create.$reason")
      def discard(reason: String) = inc(s"mod.report.discard.$reason")
    }
    object log {
      val create = inc("mod.log.create")
    }
    object irwin {
      val report = inc("mod.report.irwin.report")
      val mark = inc("mod.report.irwin.mark")
      def ownerReport(name: String) = inc(s"mod.irwin.owner_report.$name")
      def streamEventType(name: String) = inc(s"mod.irwin.streama.event_type.$name") // yes there's a typo
    }
  }
  object relay {
    val ongoing = rec("relay.ongoing")
    val moves = incX("relay.moves")
    object sync {
      def result(res: String) = inc(s"relay.sync.result.$res")
      object duration {
        val each = rec("relay.sync.duration.each")
      }
    }
  }
  object bot {
    def moves(username: String) = inc(s"bot.moves.$username")
    def chats(username: String) = inc(s"bot.chats.$username")
  }
  object cheat {
    val cssBot = inc("cheat.css_bot")
    val holdAlert = inc("cheat.hold_alert")
    object autoAnalysis {
      def reason(r: String) = inc(s"cheat.auto_analysis.reason.$r")
    }
    object autoMark {
      val count = inc("cheat.auto_mark.count")
    }
    object autoReport {
      val count = inc("cheat.auto_report.count")
    }
  }
  object email {
    object types {
      val resetPassword = inc("email.reset_password")
      val magicLink = inc("email.magic_link")
      val fix = inc("email.fix")
      val change = inc("email.change")
      val confirmation = inc("email.confirmation")
    }
    val disposableDomain = rec("email.disposable_domain")
    object actions {
      val send = inc("email.send")
      val fail = inc("email.fail")
      val retry = inc("email.retry")
    }
  }
  object security {
    object tor {
      val node = rec("security.tor.node")
    }
    object firewall {
      val block = inc("security.firewall.block")
      val ip = rec("security.firewall.ip")
      val prints = rec("security.firewall.prints")
    }
    object proxy {
      object request {
        val success = inc("security.proxy.success")
        val failure = inc("security.proxy.failure")
        val time = rec("security.proxy.request")
      }
      val percent = rec("security.proxy.percent")
    }
    object rateLimit {
      def generic(key: String) = inc(s"security.rate_limit.generic.$key")
    }
    object linearLimit {
      def generic(key: String) = inc(s"security.linear_limit.generic.$key")
    }
    object dnsApi {
      object mx {
        val time = rec("security.dnsApi.mx.time")
        val count = inc("security.dnsApi.mx.count")
        val error = inc("security.dnsApi.mx.error")
      }
      object a {
        val time = rec("security.dnsApi.a.time")
        val count = inc("security.dnsApi.a.count")
        val error = inc("security.dnsApi.a.error")
      }
    }
    object checkMailApi {
      val count = inc("checkMail.fetch.count")
      val block = inc("checkMail.fetch.block")
      val error = inc("checkMail.fetch.error")
    }
  }
  object tv {
    object stream {
      val count = rec("tv.streamer.count")
      def name(n: String) = rec(s"tv.streamer.name.$n")
    }
  }
  object relation {
    val follow = inc("relation.follow")
    val unfollow = inc("relation.unfollow")
    val block = inc("relation.block")
    val unblock = inc("relation.unblock")
  }
  object coach {
    object pageView {
      def profile(coachId: String) = inc(s"coach.page_view.profile.$coachId")
    }
  }
  object tournament {
    object pairing {
      val create = incX("tournament.pairing.create")
      val createTime = rec("tournament.pairing.create_time")
      val prepTime = rec("tournament.pairing.prep_time")
      val cutoff = inc("tournament.pairing.cutoff")
      val giveup = inc("tournament.pairing.giveup")
    }
    val created = rec("tournament.created")
    val started = rec("tournament.started")
    val player = rec("tournament.player")
    object startedOrganizer {
      val tickTime = rec("tournament.started_organizer.tick_time")
      val waitingUsersTime = rec("tournament.started_organizer.waiting_users_time")
    }
    object createdOrganizer {
      val tickTime = rec("tournament.created_organizer.tick_time")
    }
    def apiShowPartial(partial: Boolean) = inc(s"tournament.api.show.partial.$partial")
    val trouperCount = rec("tournament.trouper.count")
  }
  object plan {
    object amount {
      val paypal = incX("plan.amount.paypal")
      val stripe = incX("plan.amount.stripe")
    }
    object count {
      val paypal = inc("plan.count.paypal")
      val stripe = inc("plan.count.stripe")
    }
    val goal = rec("plan.goal")
    val current = rec("plan.current")
    val percent = rec("plan.percent")
  }
  object forum {
    object post {
      val create = inc("forum.post.create")
    }
    object topic {
      val view = inc("forum.topic.view")
    }
  }
  object puzzle {
    object selector {
      val count = inc("puzzle.selector.count")
      val time = rec("puzzle.selector.time")
      def vote(v: Int) = rec("puzzle.selector.vote")(1000 + v) // vote sum of selected puzzle
    }
    object batch {
      object selector {
        val count = incX("puzzle.batch.selector.count")
        val time = rec("puzzle.batch.selector.time")
      }
      val solve = incX("puzzle.batch.solve")
    }
    object round {
      val user = inc("puzzle.attempt.user")
      val anon = inc("puzzle.attempt.anon")
      val mate = inc("puzzle.attempt.mate")
      val material = inc("puzzle.attempt.material")
    }
    object vote {
      val up = inc("puzzle.vote.up")
      val down = inc("puzzle.vote.down")
    }
    val crazyGlicko = inc("puzzle.crazy_glicko")
  }
  object opening {
    object selector {
      val count = inc("opening.selector.count")
      val time = rec("opening.selector.time")
    }
    val crazyGlicko = inc("opening.crazy_glicko")
  }
  object game {
    def finish(status: String) = inc(s"game.finish.$status")
    object create {
      def variant(v: String) = inc(s"game.create.variant.$v")
      def speed(v: String) = inc(s"game.create.speed.$v")
      def source(v: String) = inc(s"game.create.source.$v")
      def mode(v: String) = inc(s"game.create.mode.$v")
    }
    val fetch = inc("game.fetch.count")
    val fetchLight = inc("game.fetchLight.count")
    val loadClockHistory = inc("game.loadClockHistory.count")
    object pgn {
      final class Protocol(name: String) {
        val count = inc(s"game.pgn.$name.count")
        val time = rec(s"game.pgn.$name.time")
      }
      object oldBin {
        val encode = new Protocol("oldBin.encode")
        val decode = new Protocol("oldBin.decode")
      }
      object huffman {
        val encode = new Protocol("huffman.encode")
        val decode = new Protocol("huffman.decode")
      }
    }
    val idCollision = inc("game.id_collision")
  }
  object chat {
    val message = inc("chat.message")
    val trollTrue = inc("chat.message.troll.true")
  }
  object push {
    object register {
      def in(platform: String) = inc(s"push.register.in.$platform")
      def out = inc(s"push.register.out")
    }
    object send {
      def move(platform: String) = inc(s"push.send.$platform.move")()
      def takeback(platform: String) = inc(s"push.send.$platform.takeback")()
      def corresAlarm(platform: String) = inc(s"push.send.$platform.corresAlarm")()
      def finish(platform: String) = inc(s"push.send.$platform.finish")()
      def message(platform: String) = inc(s"push.send.$platform.message")()
      object challenge {
        def create(platform: String) = inc(s"push.send.$platform.challenge_create")()
        def accept(platform: String) = inc(s"push.send.$platform.challenge_accept")()
      }
    }
    def googleTokenTime = rec("push.send.google-token")
  }
  object fishnet {
    object client {
      def result(client: String, skill: String) = new {
        def success = apply("success")
        def failure = apply("failure")
        def weak = apply("weak")
        def timeout = apply("timeout")
        def notFound = apply("not_found")
        def notAcquired = apply("not_acquired")
        def abort = apply("abort")
        private def apply(r: String) = inc(s"fishnet.client.result.$skill.$client.$r")
      }
      object status {
        val enabled = rec("fishnet.client.status.enabled")
        val disabled = rec("fishnet.client.status.disabled")
      }
      def skill(v: String) = rec(s"fishnet.client.skill.$v")
      def version(v: String) = rec(s"fishnet.client.version.${makeVersion(v)}")
      def stockfish(v: String) = rec(s"fishnet.client.engine.stockfish.${makeVersion(v)}")
      def python(v: String) = rec(s"fishnet.client.python.${makeVersion(v)}")
    }
    object queue {
      def db(skill: String) = rec(s"fishnet.queue.db.$skill")
      def sequencer(skill: String) = rec(s"fishnet.queue.sequencer.$skill")
    }
    object acquire {
      def time(skill: String) = rec(s"fishnet.acquire.skill.$skill")
      def timeout(skill: String) = inc(s"fishnet.acquire.timeout.skill.$skill")
    }
    object work {
      def acquired(skill: String) = rec(s"fishnet.work.$skill.acquired")
      def queued(skill: String) = rec(s"fishnet.work.$skill.queued")
      def forUser(skill: String) = rec(s"fishnet.work.$skill.for_user")
    }
    object analysis {
      def by(client: String) = new {
        def hash = rec(s"fishnet.analysis.hash.$client")
        def threads = rec(s"fishnet.analysis.threads.$client")
        def movetime = rec(s"fishnet.analysis.movetime.$client")
        def node = rec(s"fishnet.analysis.node.$client")
        def nps = rec(s"fishnet.analysis.nps.$client")
        def depth = rec(s"fishnet.analysis.depth.$client")
        def pvSize = rec(s"fishnet.analysis.pv_size.$client")
        def pvTotal = incX(s"fishnet.analysis.pvs.total.$client")
        def pvShort = incX(s"fishnet.analysis.pvs.short.$client")
        def pvLong = incX(s"fishnet.analysis.pvs.long.$client")
        def totalMeganode = incX(s"fishnet.analysis.total.meganode.$client")
        def totalSecond = incX(s"fishnet.analysis.total.second.$client")
        def totalPosition = incX(s"fishnet.analysis.total.position.$client")
      }
      val post = rec("fishnet.analysis.post")
      val requestCount = inc("fishnet.analysis.request")
      val evalCacheHits = rec("fishnet.analysis.eval_cache_hits")
    }
    object http {
      def acquire(skill: String) = new {
        def hit = inc(s"fishnet.http.acquire.$skill.hit")
        def miss = inc(s"fishnet.http.acquire.$skill.miss")
      }
    }
  }
  object api {
    object userGames {
      val cost = incX("api.user-games.cost")
    }
    object users {
      val cost = incX("api.users.cost")
    }
    object game {
      val cost = incX("api.game.cost")
    }
    object activity {
      val cost = incX("api.activity.cost")
    }
  }
  object export {
    object pgn {
      def game = inc("export.pgn.game")
      def study = inc("export.pgn.study")
      def studyChapter = inc("export.pgn.study_chapter")
    }
    object png {
      def game = inc("export.png.game")
      def puzzle = inc("export.png.puzzle")
    }
    def pdf = inc("export.pdf.game")
  }
  object jsmon {
    val socketGap = inc("jsmon.socket_gap")
    val unknown = inc("jsmon.unknown")
  }
  object palantir {
    val channels = rec("palantir.channels.nb")
  }
  object bus {
    val classifiers = rec("bus.classifiers")
    val subscribers = rec("bus.subscribers")
  }

  def measure[A](path: RecPath)(op: => A): A = measureRec(path(this))(op)
  def measureRec[A](rec: Rec)(op: => A): A = {
    val start = System.nanoTime()
    val res = op
    rec(System.nanoTime() - start)
    res
  }
  def measureIncMicros[A](path: IncXPath)(op: => A): A = {
    val start = System.nanoTime()
    val res = op
    path(this)(((System.nanoTime() - start) / 1000).toInt)
    res
  }

  def since[A](path: RecPath)(start: Long) = path(this)(System.nanoTime() - start)

  type Rec = Long => Unit
  type Inc = () => Unit
  type IncX = Int => Unit
  type Rate = Double => Unit

  type RecPath = lila.mon.type => Rec
  type IncPath = lila.mon.type => Inc
  type IncXPath = lila.mon.type => IncX

  def recPath(f: lila.mon.type => Rec): Rec = f(this)
  def incPath(f: lila.mon.type => Inc): Inc = f(this)

  private def inc(metric: String): Inc =
    Kamon.counter(metric).withoutTags.increment _

  private def inc(metric: String, tag: String, value: String): Inc =
    Kamon.counter(metric).withTag(tag, value).increment _

  private def incX(name: String): IncX =
    incCounter(Kamon.counter(name).withoutTags)

  private def incX(name: String, tag: String, tagValue: String): IncX =
    incCounter(Kamon.counter(name).withTag(tag, tagValue))

  private def incCounter(counter: kamon.metric.Counter): IncX = value => {
    if (value < 0) logger.warn(s"Negative counter value: $counter=$value")
    else counter.increment(value)
  }

  private def rec(name: String): Rec =
    recGauge(Kamon.gauge(name).withoutTags)

  private def rec(name: String, tag: String, tagValue: String): Rec =
    recGauge(Kamon.gauge(name).withTag(tag, tagValue))

  private def recGauge(gauge: kamon.metric.Gauge): Rec = value => {
    if (value < 0) logger.warn(s"Negative gauge value: $gauge=$value")
    else gauge.update(value)
  }

  // to record Double rates [0..1],
  // we multiply by 100,000 and convert to Int [0..100000]
  // private def rate(name: String): Rate = value => {
  //   Kamon.gauge(name).withoutTags.update((value * 100000).toInt)
  // }
  private def rate(name: String, tag: String, tagValue: String): Rate = value => {
    Kamon.gauge(name).withTag(tag, tagValue).update((value * 100000).toInt)
  }

  private val stripVersionRegex = """[^\w\.\-]""".r
  private def stripVersion(v: String) = stripVersionRegex.replaceAllIn(v, "")
  private def nodots(s: String) = s.replace('.', '_')
  private val makeVersion = nodots _ compose stripVersion _

  private val logger = lila.log("monitor")
}
