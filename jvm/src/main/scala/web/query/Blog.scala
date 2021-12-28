package com.ryanwhittingham.web.query.blog

import com.ryanwhittingham.web.config.Config.config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.getquill._

object QueryBlog {
  // Initialize db connection
  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setUser(config.dbUser)
  pgDataSource.setDatabaseName(config.dbName)
  pgDataSource.setPortNumber(config.dbPort)
  pgDataSource.setPassword(config.dbPassword)

  val dbConfig = new HikariConfig()
  dbConfig.setDataSource(pgDataSource)

  val ctx = new PostgresJdbcContext(LowerCase, new HikariDataSource(dbConfig))

  case class Blog(
      id: Int,
      tstamp: Int,
      title: String,
      author: String,
      contents: String,
      org_hash: String
  )

  def queryByTitle(title: String): Option[Blog] = {
    import ctx._
    val blog = ctx.run(query[Blog].filter(_.title == lift(title)))
    if (blog.length == 0) {
      None
    }
    Some(blog(0))
  }
}
