package com.ryanwhittingham.web.templates

import com.ryanwhittingham.web.search.BlogSearchResult
import com.ryanwhittingham.web.tags.Tags
import scalatags.Text.all._

import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date

object Head {
  val basicHead = head(
    // character encoding
    meta(charset := "utf-8"),
    meta(name := "viewport", content := "width=device-width, initial-scale=1"),
    // highlight.js
    link(
      rel := "stylesheet",
      href := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.3.1/styles/default.min.css"
    ),
    script(
      src := "/static/highlight.min.js"
    ),
    // Bootstrap CSS, including dark theme
    link(
      rel := "stylesheet",
      href := "https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css"
    ),
    link(
      rel := "stylesheet",
      href := "https://cdn.jsdelivr.net/npm/@forevolve/bootstrap-dark@1.0.0/dist/css/toggle-bootstrap.min.css"
    ),
    link(
      rel := "stylesheet",
      href := "https://cdn.jsdelivr.net/npm/@forevolve/bootstrap-dark@1.0.0/dist/css/toggle-bootstrap-dark.min.css"
    ),
    // Bootstrap icons
    link(
      rel := "stylesheet",
      href := "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.3.0/font/bootstrap-icons.css"
    ),
    // Own CSS
    link(
      rel := "stylesheet",
      href := "/static/ryw.css"
    )
  )
}

object Body {
  // First jQuery, then Popper, then Bootstrap JS
  val bootstrapJS = Seq(
    script(src := "https://code.jquery.com/jquery-3.3.1.slim.min.js"),
    script(
      src := "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js"
    ),
    script(
      src := "https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js"
    )
  )

  // This refers to the library in this project written in ScalaJS
  val ownJS = Seq(
    script(src := "/static/ryw-web-opt-bundle.js"),
    script(src := "/static/ryw.js")
  )
}

object MainTemplate {
  def fill(contents: scalatags.Text.Modifier) = {
    val headContents = Head.basicHead
    val jsBodyEnd = Body.bootstrapJS ++ Body.ownJS

    html(
      headContents,
      body(
        div(
          `class` := "container",
          Navbar.navbar,
          div(
            `class` := "row g-5",
            div(
              `class` := "pb-3 pt-3 col-md-8",
              contents
            ),
            Sidebar.sidebar
          )
        ),
        Modal.searchTooLong,
        jsBodyEnd
      )
    )
  }
}

object ErrTemplates {
  def notFound() = {
    MainTemplate.fill("Nothing found here!")
  }

  def appError() = {
    MainTemplate.fill("An application error occurred.")
  }
}

object Navbar {
  // Tuples are link, then text
  private val navbarContents = Seq(
    ("/blog", "Home"),
    ("/about", "About"),
    ("/cv", "CV"),
    ("/contact", "Contact")
  )
  private val aClass = "nav-item nav-link text-center"
  private val navBarTags =
    navbarContents.map(x => a(`class` := aClass, href := x._1, x._2))

  val navbar = Tags.nav(
    `class` := "nav nav-pills nav-fill bg-light",
    navBarTags
  )
}

object Sidebar {
  def getLastMonths(n: Integer) = {
    // Fetch current date
    val formatter = new SimpleDateFormat("MM yyyy")
    val monthDate = DateTimeFormatter.ofPattern("MM yyyy")
    val date = new Date()
    val currentMonth = formatter.format(date)
    val start = YearMonth.parse(currentMonth, monthDate)

    // Formats for link text & href
    val outFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
    val hrefFormat = DateTimeFormatter.ofPattern("yyyy-MM")

    // List of month links
    (0 to n)
      .map(x =>
        li(
          a( // Note that this class is used to correct the links by the
            // makeHrefRelativeToLocation JS function
            href := s"/blog-by-month/${start.minusMonths(x).format(hrefFormat)}",
            start.minusMonths(x).format(outFormat)
          )
        )
      )
      .toList
  }

  val sidebar = div(
    `class` := "p-4",
    style := "top: 2rem;",
    div(`class` := "mb-3 rounded", Search.search),
    h4(`class` := "mb-3", "Archives"),
    ol(`class` := "list-unstyled", getLastMonths(12)),
    h4(`class` := "mb-3", "External"),
    ol(
      `class` := "list-unstyled",
      li(
        a(href := "https://github.com/ryw89", Tags.i(`class` := "bi bi-github"))
      ),
      li(
        a(
          href := "https://www.linkedin.com/in/ryan-whittingham-81654516b/",
          Tags.i(`class` := "bi bi-linkedin")
        )
      )
    )
  )
}

object Badge {
  def makeBadges(tags: Seq[String]) = {
    val badgeClass = "badge badge-light mx-1"
    for {
      (tag) <- tags
    } yield {
      a(href := s"/search/${tag}", `class` := badgeClass, tag)
    }
  }
}

object Blog {
  def blog(
      title: String,
      html: String,
      date: String,
      tags: Seq[String] = Seq()
  ) = {
    val tagsHtml = Badge.makeBadges(tags)

    Tags.article(
      `class` := "blog-post",
      div(
        `class` := "border-bottom pb-2 mb-3",
        h1(`class` := "blog-post-title", title),
        Tags.time(`class` := "pb-2", date),
        " | ",
        tagsHtml
      ),
      raw(html)
    )
  }
}

object Modal {
  def searchTooLong() = {
    div(
      `class` := "modal fade",
      id := "search-too-long-modal",
      tabindex := "-1",
      role := "dialog",
      div(
        `class` := "modal-dialog modal-dialog-centered",
        role := "document",
        div(
          `class` := "modal-content",
          div(`class` := "modal-body", "Search query is too long.")
        )
      )
    )

  }
}

object Search {
  val search = div(
    `class` := "input-group mb-3",
    input(
      id := "blog-search-input",
      `type` := "text",
      `class` := "form-control",
      placeholder := "Search..."
    ),
    div(
      `class` := "input-group-append",
      button(
        id := "blog-search-button",
        `class` := "btn btn-outline-secondary",
        `type` := "button",
        Tags.i(`class` := "bi bi-search blog")
      )
    )
  )

}

object SearchResults {
  def searchResults(blogSearchResults: List[BlogSearchResult]) = {
    val blogTitles = blogSearchResults.map(_.title)
    val blogDates = blogSearchResults.map(_.date)
    val blogTagHtmls = blogSearchResults.map(_.tags).map(Badge.makeBadges(_))
    val blogLinks =
      blogTitles.map(_.toLowerCase.replace(" ", "-")).map(s => "/blog/" + s)

    val blogHref =
      for {
        (title, link, date, tags) <-
          blogTitles
            .lazyZip(blogLinks)
            .lazyZip(blogDates)
            .lazyZip(blogTagHtmls)
            .toList
      } yield {
        li(
          h2(`class` := "display-inline", a(href := link, title)),
          div(`class` := "pb-3", Tags.time(date), " | ", tags)
        )
      }

    // Build list of blog titles & links
    MainTemplate.fill(
      Seq(
        h1(`class` := "pb-4 mb-4 border-bottom", "Search results"),
        ol(`class` := "list-unstyled", blogHref),
        script("document.title = 'Search results'")
      )
    )
  }

  def noSearchResults() = {
    MainTemplate.fill(
      Seq(
        h1(`class` := "pb-4 mb-4 border-bottom", "Search results"),
        h2("No search results found."),
        script("document.title = 'Search results'")
      )
    )
  }
}
