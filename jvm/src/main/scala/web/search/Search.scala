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
import scala.util.Try

object Conf {
  private val appDirs = AppDirsFactory.getInstance();
  val searchIndexDir =
    os.Path(appDirs.getUserDataDir("ryw-web", null, null)) / "search"
  val index = new NIOFSDirectory(Paths.get(searchIndexDir.toString))
  val config = new IndexWriterConfig()
  val writer = new IndexWriter(index, config)
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
  import com.ryanwhittingham.web.db.Db._
  import ctx._
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
    info(s"Number of hits: ${blogIds.length}")

    if (blogIds.length == 0) {
      return None
    }

    // Get blog titles & dates & build output HTML
    val blogTitlesAndTimestamps: List[(String, Int)] =
      ctx.run(
        ctx
          .query[Blog]
          .filter(b => lift(blogIds).contains(b.id))
          .map(b => (b.title, b.tstamp))
      )

    val blogTitles = blogTitlesAndTimestamps.unzip._1
    val blogDates =
      blogTitlesAndTimestamps.unzip._2.map(unixTimeToDate(_, "MMM d, y"))

    // Make HTML tags
    val out: scalatags.Text.TypedTag[String] =
      searchResults(blogTitles, blogDates)
    Some(out)
  }

}
