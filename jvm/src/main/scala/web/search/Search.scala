package com.ryanwhittingham.web.search

import com.ryanwhittingham.web.common.UnixTimeToDate.unixTimeToDate
import com.ryanwhittingham.web.templates.SearchResults.searchResults
import net.harawata.appdirs.AppDirsFactory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, StoredField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.NIOFSDirectory
import wvlet.log.LogSupport

import java.net.URLDecoder
import java.nio.file.Paths
import scala.collection.mutable.Map
import scala.util.Try

object Conf {
  private val appDirs = AppDirsFactory.getInstance();
  val searchIndexDir =
    os.Path(appDirs.getUserDataDir("ryw-web", null, null)) / "search"
  val index = new NIOFSDirectory(Paths.get(searchIndexDir.toString))
  val config = new IndexWriterConfig()
  val writer = new IndexWriter(index, config)
}

/** Object with wrapper function around BlogSearchResults' methods. */
object GetBlogSearchResults {
  def get(blogIdsAndScores: Seq[(Int, Float)]) = {
    val bsr = new BlogSearchResults(blogIdsAndScores)
    bsr.getTitlesAndDates()
    bsr.getTags()
    bsr.makeBlogSearchResults()
    bsr.res
  }
}

/** Simple data class for encapsulating fields of a blog search result. */
class BlogSearchResult(
    val id: Int,
    val title: String,
    val date: String,
    val score: Float,
    val tags: Seq[String] = Seq()
)

/** Fetch needed blog post data from database to help in building
  * search results page. */
class BlogSearchResults(blogIdsAndScores: Seq[(Int, Float)]) {
  import com.ryanwhittingham.web.db.Db._
  import ctx._

  var res: List[BlogSearchResult] = List()

  private val ids: List[Int] = blogIdsAndScores.map(_._1).toList
  private val scores: Map[Int, Float] = makeIdScoresMap()

  private var blogIdsTitlesAndDates: List[(Int, String, String)] = List()
  private var tags: Map[Int, Seq[String]] = Map()

  def makeIdScoresMap() = {
    var out: Map[Int, Float] = Map()
    for ((id, score) <- blogIdsAndScores) {
      out = out + (id -> score)
    }
    out
  }

  def getTitlesAndDates() = {
    val res: List[(Int, String, Int)] =
      ctx.run(
        ctx
          .query[Blog]
          .filter(b => lift(ids).contains(b.id))
          .sortBy(b => b.id)
          .map(b => (b.id, b.title, b.tstamp))
      )

    if (res.length > 0) {
      blogIdsTitlesAndDates = for {
        (r: (Int, String, Int)) <- res
      } yield {
        val id = r._1
        val title = r._2
        val date = unixTimeToDate(r._3, "MMM d, y")
        (id, title, date)
      }
    }
  }

  def getTags() = {
    val res: List[(Int, String)] =
      ctx.run(
        ctx
          .query[Tag]
          .filter(t => lift(ids).contains(t.blog_id))
          .map(t => (t.blog_id, t.tag))
      )

    // Iterate over unique blog IDs and build tags map
    for (id: Int <- res.map(_._1).distinct) {
      val tagsForThisId: List[String] = res.filter(_._1 == id).map(_._2)
      if (tagsForThisId.length > 0) {
        tags = tags + (id -> tagsForThisId)
      }
    }

  }

  def makeBlogSearchResults() = {
    val unsortedRes = for {
      (id, title, date) <- blogIdsTitlesAndDates
    } yield {
      // Fetch scores
      val score = scores.get(id).get

      // Fetch tags
      val t = tags.get(id) match {
        case Some(t) => t
        case None    => Seq()
      }

      // Construct BlogSearchResult object & append to res public
      // member
      new BlogSearchResult(id, title, date, score, t)
    }
    res = unsortedRes.sortBy(_.score)
  }

}

/** Methods for updating Lucene search index. */
class UpdateIndex(val blogIds: Option[Seq[Int]] = None) {
  def addBlogsToIndex(): Try[Unit] =
    Try {
      import com.ryanwhittingham.web.db.Db._
      import ctx._
      // Fetch all data in blog table
      val blog: Seq[Blog] = ctx.run(ctx.query[Blog])

      // Filter blogs to contain only desired ids
      val filteredBlogs: Seq[Blog] = blogIds match {
        case Some(blogIds) => blog.filter(blogIds contains _.id)
        case None          => Seq()
      }

      for (b <- filteredBlogs) {
        // Fetch tags, if any
        val tags: Seq[Tag] =
          ctx.run(ctx.query[Tag].filter(_.blog_id == lift(b.id)))

        // Make a new Lucene document & fill in
        val doc = new Document()
        doc.add(new StoredField("id", b.id))
        doc.add(new TextField("contents", b.contents, Field.Store.NO))

        if (tags.length > 0) {
          val allTags = tags.map(_.tag).mkString(" ")
          doc.add(new TextField("tag", allTags, Field.Store.YES))
        }

        Conf.writer.addDocument(doc)
        Conf.writer.commit()
      }

    }
}

/** Search Lucene index. */
class Search(val query: String) extends LogSupport {
  info(s"Original search query: $query")
  private val cleanedQuery =
    URLDecoder.decode(query).replaceAll("[^a-zA-Z0-9]", "")
  info(s"Cleaned query: $cleanedQuery")

  def queryIsValid(): Boolean = {
    if (query.length > 32) {
      false
    }
    true
  }

  def search(): Option[scalatags.Text.TypedTag[String]] = {
    info(s"Performing search with Lucene index at ${Conf.searchIndexDir}.")
    val reader = DirectoryReader.open(Conf.writer)
    val analyzer = new StandardAnalyzer

    // Perform Lucene search
    val queryParser =
      new MultiFieldQueryParser(Array("contents", "tag"), analyzer).parse(query)
    info(s"Using query: ${queryParser}.")
    val searcher = new IndexSearcher(reader)
    val docs = searcher.search(queryParser, 10)
    val hits = docs.scoreDocs

    // Get unique blog post IDs (IDs in SQL database, that is)
    val blogIds: List[Int] = {
      for {
        (hit) <- hits
      } yield {
        val doc = searcher.doc(hit.doc)
        doc.get("id").toInt
      }
    }.sortBy(hits.map(_.score)).distinct.toList

    val blogIdsAndScores: List[(Int, Float)] = {
      for {
        (hit) <- hits
      } yield {
        val doc = searcher.doc(hit.doc)
        (doc.get("id").toInt, hit.score)
      }
    }.distinct.toList

    info(s"Number of hits: ${blogIds.length}")

    if (blogIds.length == 0) {
      return None
    }

    val blogSearchResults = GetBlogSearchResults.get(blogIdsAndScores)

    // Make HTML tags
    val out: scalatags.Text.TypedTag[String] =
      searchResults(blogSearchResults)
    Some(out)
  }

}
