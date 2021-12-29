package com.ryanwhittingham.web.orgtohtml

import com.ryanwhittingham.web.common.{Hash, Unwrap}
import com.ryanwhittingham.web.config.Config
import com.ryanwhittingham.web.db.Db._
import wvlet.log.LogSupport

import java.lang.UnsupportedOperationException
import java.text.SimpleDateFormat
import scala.io.Source
import scala.util.Try

object OrgToHtmlToDb extends LogSupport {

  /** Inserts blog post data to database, given a .org file path.
    * Largely a wrapper around OrgToHtml classes and its methods. */
  def orgToHtmlToDb(orgPath: String): Try[Unit] =
    Try {
      val o = new OrgToHtml(orgPath)
      o.orgToHtml()
      o.hashOrgFile()
      info(s"Org file hash: ${o.orgHash}.\n")
      o.extractOrgFields()
      o.createId()

      // Construct Blog object, unwrapping Options as needed
      val id = o.id.get.toInt
      val tstamp = o.tstamp.get
      val title = o.title.get
      val author = o.author
      val contents = o.contents.get
      val orgHash = o.orgHash.get

      val blog = Blog(id, tstamp, title, author, contents, orgHash)
      insertBlogPostToDb(blog)

    }

  /** Insert a row to blog table. */
  def insertBlogPostToDb(blog: Blog): Unit = {
    import ctx._
    // First, check for org_hash to see if the file is already in the database.
    val orgHashes: Seq[String] = ctx.run(query[Blog].map(_.org_hash))
    if (orgHashes contains blog.org_hash) {
      throw new RuntimeException(
        s".org file with hash ${blog.org_hash} is already in the database."
      )
    }
    ctx.run(query[Blog].insert(lift(blog)))
    info("Inserted new blog post into database.")
  }
}

class OrgToHtml(orgPath: String) extends LogSupport {
  // Blog fields for later insertion into database
  var id: Option[Int] = None
  var tstamp: Option[Int] = None
  var title: Option[String] = None
  var author: Option[String] = None
  var contents: Option[String] = None
  var orgHash: Option[String] = None

  // Full path to org file
  val fullOrgPath = os.Path(Config.config.blogRoot) / orgPath

  // org file contents
  val orgContents = Source.fromFile(fullOrgPath.toString).getLines.mkString

  /** Method for converting .org file to .html using Emacs. */
  def orgToHtml(): Try[Unit] =
    Try {
      info(s"Requesting invocation of org2html.sh with filename ${orgPath}.")
      val wd = Config.config.org2htmlDir
      val p =
        os.proc("bash", "org2html.sh", orgPath, "out.html")
          .call(cwd = os.Path(wd), check = true)

      val stdout = p.out.string
      val stderr = p.err.string

      if (stderr.nonEmpty) {
        info(s"stderr of org2html.sh:\n${stderr}")
      }

      // Fetch output HTML file from stdout
      val htmlPath = stdout.split("Output HTML at: ")(1).trim.stripSuffix(".")
      info(s"HTML path at: ${htmlPath}.")
      contents = Some(Source.fromFile(htmlPath).getLines.mkString)
    }

  def getRawOrgField(
      orgLines: List[String],
      start: String,
      delim: String
  ): Try[Option[String]] =
    Try {
      val out =
        orgLines.filter(_ startsWith start).map(_.split(delim)(1).trim).toList

      if (out.length == 1) {
        Some(out(0))
      } else if (out.length == 0) {
        None
      } else {
        throw new RuntimeException(
          s"Found more than one org field beginning with '${start}'"
        )
      }

    }

  /** Parse an org-mode date and convert to Unix time. */
  def orgDateToUnixTime(date: String): Int = {
    // Example date: [2021-12-20 Mon 13:42]

    // First, remove surrounding square brackets
    val cleanedDate = date.replaceAll("[\\[\\]]", "")

    // Now parse & convert to seconds since epoch
    val parsed = new SimpleDateFormat("yyyy-MM-dd EEE HH:mm").parse(cleanedDate)
    val unixTime = parsed.getTime() / 1000
    Math.round(unixTime)
  }

  /** Parse relevant data out of Emacs org file. */
  def extractOrgFields(): Try[Unit] =
    Try {
      val orgLines = Source.fromFile(fullOrgPath.toString).getLines.toList

      val rawTitle = getRawOrgField(orgLines, "#+TITLE", ": ")
      info(s"Found title ${rawTitle}.")
      title = Some(Unwrap.unwrapTryOptionOrFail(rawTitle))

      val rawDate = getRawOrgField(orgLines, "#+DATE", ": ")
      info(s"Found date ${rawDate}.")
      tstamp = Some(orgDateToUnixTime(Unwrap.unwrapTryOptionOrFail(rawDate)))
      info(s"Converted date to ${tstamp}.")

      val rawAuthor = getRawOrgField(orgLines, "#+AUTHOR", ": ")
      info(s"Found author ${rawAuthor}.")

      // Will throw if Failure, which is desired behavior
      author = rawAuthor.get
    }

  /** Hash the .org file for later insertion into database. */
  def hashOrgFile(): Unit = {
    orgHash = Some(Hash.md5HashString((orgContents)))
  }

  /** Creates an incremental ID for this blog post for later insertion
    * into the database. */
  def createId(): Unit = {
    import ctx._
    val maxId: Int =
      try {
        ctx.run(query[Blog].map(_.id)).max
      } catch {
        // This UnsupportedOperationException exception will occur if
        // the database is empty, as it is not possible to get the max
        // of an empty iterable.
        case _: UnsupportedOperationException => 0
        case other: Throwable                 => throw other
      }
    id = Some(maxId + 1)
  }
}
