package com.ryanwhittingham.web.query.blog

import wvlet.log.Logger

object QueryBlog {
  import com.ryanwhittingham.web.db.Db._
  private val logger = Logger.of[App]
  def mostRecentBlog(): String = {
    logger.info("Fetching most recent blog post.")
    import ctx._
    // Fetch title of highest id
    ctx.run(query[Blog].map(b => (b.tstamp, b.title))).maxBy(_._1)._2
  }

  def queryByTitle(title: String): Option[(Blog, Seq[String])] = {
    logger.info(s"Searching for blog post with title '${title}'.")
    import ctx._
    // First, fetch all IDs and titles
    val blogIdsAndTitles: Seq[(Int, String)] =
      ctx.run(query[Blog].map(b => (b.id, b.title)))

    // Note that element 1 of tuple is id, and element 2 is title.
    // This filter and map operation finds the blog IDs that match the
    // title. Additionally, we replace dashes with spaces to allow
    // dashes in request.
    val matchedBlogIds =
      blogIdsAndTitles
        .filter(_._2.toLowerCase == title.toLowerCase.replace("-", " "))
        .map(_._1)

    // Nothing found in this case, so return a None to the caller --
    // this will be presented as a 404 upstream
    if (matchedBlogIds.length == 0) {
      logger.warn("None found.")
      return None
    }

    val matchedId = matchedBlogIds(0)
    val blog = ctx.run(query[Blog].filter(_.id == lift(matchedId)))(0)
    logger.info(s"Found matched blog post with id ${blog.id}.")

    val tag: Seq[String] =
      ctx.run(query[Tag].filter(_.blog_id == lift(blog.id))).map(_.tag)

    Some(blog, tag)
  }
}
