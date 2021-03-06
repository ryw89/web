package com.ryanwhittingham.web

import com.thedeanda.lorem.LoremIpsum
import scalatags.Text.all._
import wvlet.log.Logger

import scala.util.{Failure, Success}

import common.UnixTimeToDate.unixTimeToDate
import config.Config
import orgtohtml.OrgToHtmlToDb
import query.blog.QueryBlog.{mostRecentBlog, queryByTitle}
import search.{
  BlogsByDateRange,
  PreviousOrNextBlogPostTitle,
  Search,
  UpdateIndex
}
import templates.{Blog, ErrTemplates, MainTemplate, SearchResults}

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
      case Success(id) => {
        val respJson = ujson
          .Obj(
            "blogId" -> id,
            "msg" -> "Successfully added .org file to database."
          )
          .toString
        cask.Response(respJson, 200)
      }
      case Failure(f) => {
        logger.error(f)
        cask.Response("An application error occurred.\n", 500)
      }
    }
  }

  @cask.get("/api/index-post/:postId")
  def indexPost(postId: Int) = {
    val u = new UpdateIndex(Some(Seq(postId)))
    u.addBlogsToIndex() match {
      case Success(_) =>
        cask.Response("Successfully indexed blog post.\n", 200)
      case Failure(f) => {
        logger.error(f)
        cask.Response("An application error occurred.\n", 500)
      }
    }
  }

  @cask.get("/api-public/prev-title/:postTitle")
  def getPreviousTitle(postTitle: String) = {
    val title: Option[String] =
      PreviousOrNextBlogPostTitle.get(postTitle, next = false)
    title match {
      case Some(t) => ujson.Obj("title" -> t)
      case None    => ujson.Obj("title" -> ujson.Null)
    }

  }

  @cask.get("/api-public/next-title/:postTitle")
  def getNextTitle(postTitle: String) = {
    val title: Option[String] =
      PreviousOrNextBlogPostTitle.get(postTitle, next = true)
    title match {
      case Some(t) => ujson.Obj("title" -> t)
      case None    => ujson.Obj("title" -> ujson.Null)
    }

  }

  @cask.get("/blog")
  def getMostRecentBlog() = {
    postByTitle(mostRecentBlog)
  }

  @cask.get("/blog/:postTitle")
  def postByTitle(postTitle: String) = {
    queryByTitle(postTitle) match {
      case Some(blogAndTags) =>
        cask.Response(
          MainTemplate.fill(
            Blog.blog(
              blogAndTags._1.title,
              blogAndTags._1.contents,
              unixTimeToDate(blogAndTags._1.tstamp, "MMM d, y"),
              blogAndTags._2
            )
          ),
          200
        )
      case None => cask.Response(ErrTemplates.notFound, 404)
    }

  }

  @cask.get("/blog-by-month/:month")
  def postsByMonth(month: String) = {
    BlogsByDateRange.get(month + "-01") match {
      case Some(s) => cask.Response(s, 200)
      case None    => cask.Response(SearchResults.noSearchResults, 200)
    }
  }

  /** Redirect to CV asset. */
  @cask.get("/cv")
  def redirectToCvAsset() = {
    cask.Redirect("/ryan_cv.pdf")
  }

  /** Example page for examining UI. */
  @cask.get("/example")
  def example() = {
    val lorem = LoremIpsum.getInstance()
    val bodyContents = lorem.getParagraphs(50, 70)

    MainTemplate.fill(
      Blog.blog("A cool title", "Jan 01, 1900", bodyContents)
    )

  }

  @cask.get("/search/:query")
  def search(query: String) = {
    val s = new Search(query)

    if (!s.queryIsValid) {
      cask.Response(ErrTemplates.appError, 500)
    } else {
      s.search() match {
        case Some(s) => cask.Response(s, 200)
        case None    => cask.Response(SearchResults.noSearchResults, 200)
      }

    }
  }

  @cask.get("/notfound")
  def notFound() = {
    cask.Response(ErrTemplates.notFound, 200)
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
