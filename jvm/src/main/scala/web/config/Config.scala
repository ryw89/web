package com.ryanwhittingham.web.config

import net.harawata.appdirs.AppDirsFactory

/** Program-wide configuration settings. */
object Config {
  private val appDirs = AppDirsFactory.getInstance()
  private val confDir = os.Path(appDirs.getUserConfigDir("ryw-web", null, null))
  private val confPath = confDir / "config.json"
  private val confStr = os.read(confPath)

  case class Schema(
      dbHost: String,
      dbPort: Int,
      dbUser: String,
      dbPassword: String,
      dbName: String,
      dbType: String,
      port: Int,
      blogRoot: String,
      org2htmlDir: String
  )

  private implicit val SchemaRW = upickle.default.macroRW[Schema]
  val config = upickle.default.read[Schema](ujson.read(confStr))

  /** Validate configuration. */
  def testConfigLoad(): Unit = {
    val _ = config
    ()
  }
}

object Jdbc {
  import Config.config
  val jdbc =
    s"""jdbc:
     |{$config.dbType}://
     |{$config.dbHost}:|
     |{$config.dbPort}/
     |{$config.dbName}?user=
     |{$config.dbUser}&password=
     |{$config.dbPassword}&ssl=true""".stripMargin.replaceAll("\n", " ")
}
