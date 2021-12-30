package com.ryanwhittingham.web.templates

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
      src := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.3.1/highlight.min.js"
    ),
    script("hljs.highlightAll();"),
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
  val ownJS = script(src := "/static/ryw-web-opt-bundle.js")
}

object MainTemplate {
  def fill(contents: scalatags.Text.Modifier) = {
    val headContents = Head.basicHead
    val jsBodyEnd = Body.bootstrapJS ++ Seq(Body.ownJS)

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
        jsBodyEnd
      )
    )
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
            href := s"/blog/by-month/${start.minusMonths(x).format(hrefFormat)}",
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

object Blog {
  def blog(title: String, html: String) = {
    Tags.article(
      `class` := "blog-post",
      h2(`class` := "pb-4 mb-4 border-bottom blog-post-title", title),
      raw(html)
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
        Tags.i(`class` := "bi bi-search")
      )
    )
  )

}
