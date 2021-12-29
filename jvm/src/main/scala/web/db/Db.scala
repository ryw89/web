package com.ryanwhittingham.web.db

import com.ryanwhittingham.web.config.Config.config
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill._

object Db {
  // Initialize db connection
  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setUser(config.dbUser)
  pgDataSource.setDatabaseName(config.dbName)
  pgDataSource.setPortNumber(config.dbPort)

  if (!config.dbPassword.trim.isEmpty) {
    pgDataSource.setPassword(config.dbPassword)
  }

  val dbConfig = new HikariConfig()
  dbConfig.setDataSource(pgDataSource)

  val ctx = new PostgresJdbcContext(LowerCase, new HikariDataSource(dbConfig))

  // Table schemas
  case class Blog(
      id: Int,
      tstamp: Int,
      title: String,
      author: String,
      contents: String,
      org_hash: String
  )

  case class Tag(
      blog_id: Int,
      tag: String
  )

}
