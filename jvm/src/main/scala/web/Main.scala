package com.ryanwhittingham.web

import com.thedeanda.lorem.LoremIpsum
import scalatags.Text.all._
import wvlet.log.Logger

import config.Config
import errors.ServeError
import query.blog.QueryBlog.queryByTitle
import search.Search
import templates.{Blog, MainTemplate}

object App extends cask.MainRoutes {
  private val logger = Logger.of[App]

  // No point in continuing if config cannot be loaded
  Config.testConfigLoad()
  override val port = Config.config.port

  logger.info(s"Running server on port $port ...")

  @cask.get("/blog/:postTitle")
  def postByTitle(postTitle: String) = {
    val postContents = queryByTitle(postTitle)
    postContents match {
      case Some(p) => cask.Response(p.contents)
      case None    => ServeError.serve(404)
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
      cask.Response("Some content", 200)
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
