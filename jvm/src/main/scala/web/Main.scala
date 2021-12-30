package com.ryanwhittingham.web

import com.thedeanda.lorem.LoremIpsum
import scalatags.Text.all._
import wvlet.log.Logger

import scala.util.{Failure, Success}

import config.Config
import orgtohtml.OrgToHtmlToDb
import query.blog.QueryBlog.queryByTitle
import search.Search
import templates.{Blog, ErrTemplates, MainTemplate}

object App extends cask.MainRoutes {
  private val logger = Logger.of[App]

  // No point in continuing if config cannot be loaded
  Config.testConfigLoad()
  override val port = Config.config.port

  logger.info(s"Running server on port $port ...")

  /** Test route for API. */
  @cask.get("/api/test")
  def serve() = {
    "Request responded to."
  }

  @cask.get("/api/org2html/:orgPath")
  def orgToHtml(orgPath: String) = {
    OrgToHtmlToDb.orgToHtmlToDb(orgPath) match {
      case Success(_) =>
        cask.Response("Successfully added .org file to database.\n", 200)
      case Failure(f) => {
        logger.error(f)
        cask.Response("An application error occurred.\n", 500)
      }
    }
  }

  @cask.get("/blog/:postTitle")
  def postByTitle(postTitle: String) = {
    import tags.Tags._
    queryByTitle(postTitle) match {
      case Some(b) =>
        cask.Response(
          MainTemplate.fill(
            article(`class` := "blog-post", Blog.blog(b.title, b.contents))
          ),
          200
        )
      case None => cask.Response(ErrTemplates.notFound, 404)
    }

  }

  /** Example page for examining UI. */
  @cask.get("/example")
  def example() = {
    import tags.Tags._

    val lorem = LoremIpsum.getInstance()
    val bodyContents = lorem.getParagraphs(50, 70)

    MainTemplate.fill(
      article(
        `class` := "blog-post",
        Blog.blog("A cool title", bodyContents)
      )
    )

  }

  @cask.get("/search/:query")
  def search(query: String) = {
    val s = new Search(query)

    if (!s.queryIsValid) {
      cask.Redirect("/error")
    } else {
      cask.Response("Filler content", 200)
    }
  }

  @cask.get("/notfound")
  def notFound() = {
    MainTemplate.fill(
      h2("Nothing found here!")
    )

  }

  @cask.get("/error")
  def error() = {
    MainTemplate.fill(
      h2("An application error occurred.")
    )

  }

  @cask.staticResources("/static")
  def staticResourcesRoute() = {
    "."
  }

  initialize()
}
