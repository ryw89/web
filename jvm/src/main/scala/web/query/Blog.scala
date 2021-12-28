package com.ryanwhittingham.web.query.blog

object QueryBlog {
  import com.ryanwhittingham.web.db.Db._
  def queryByTitle(title: String): Option[Blog] = {
    import ctx._
    val blog = ctx.run(query[Blog].filter(_.title == lift(title)))
    if (blog.length == 0) {
      None
    }
    Some(blog(0))
  }
}
