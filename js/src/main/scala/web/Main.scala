package com.ryanwhittingham.web

import org.querki.jquery._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.TextArea

import scalajs.js
import scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel(name = "ryw") @JSExportAll
object Main {

  /** Custom handler for main search bar. Intended to be attached to a
    * mouse click handler for the search button.
    */
  def searchHandler(formInputId: String): Unit = {
    val document = dom.document
    val window = dom.window
    val formValue =
      document.getElementById(formInputId).asInstanceOf[TextArea].value

    // Validate search contents
    if (formValue.length > 32) {
      js.Dynamic.global.jQuery("#search-too-long-modal").modal()
      return ()
    }

    // URL-encode search contents for URL redirection
    var query = ""
    try {
      query =
        js.Dynamic.global.encodeURIComponent(formValue).asInstanceOf[String]
    } catch {
      case _: Throwable => {
        window.alert("Query is invalid.")
        return ()
      }
    }

    // Redirect
    val origin = window.location.origin
    dom.window.location.href = s"${origin}/search/${query}"
    return ()
  }

  /** Remove leading whitespace in <code> HTML blocks. */
  def rmPreCodeLeadingWhitespace() = {
    val pre = dom.document.getElementsByTagName("code")
    for (i <- 0 until pre.length) {
      val text = pre(i).firstChild.nodeValue
      pre(i).firstChild.nodeValue = text.stripLeading()
    }
  }

  /** Map certain languages from their Emacs org-mode name to a
    * highlight.js compatible name. */
  def mapHighlightJsLangs() = {
    val renames = Map("elisp" -> "lisp")
    for ((oldLang, newLang) <- renames) {
      $(s".lang-${oldLang}").attr("class", s"lang-${newLang}")
    }
  }

  /** Use public API to get the title of the previous or next blog post
    * and optionally redirect to its URL. This function will also
    * disable previous/next buttons if no previous or next blog post
    * is found. */
  def redirectToPreviousOrNextTitleUrl(
      postTitle: String,
      prev: Boolean,
      redirect: Boolean = false
  ) = {
    // Not the most performant ec for Scala.js, but seemingly OK for
    // this use case
    implicit val ec: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.global

    val prevOrNext = if (prev) "prev" else "next"
    val origin = dom.window.location.origin
    val url = s"${origin}/api-public/${prevOrNext}-title"
    val f = Ajax.get(s"${url}/${postTitle}")

    for (value <- f) {
      val res = value.responseText
      val titleVal = js.JSON.parse(res).title

      // title will be null if there was no previous/next blog post
      if (titleVal == null) {
        // In this case, let's disable the previous or next blog post
        // buttons found on this page. CSS styling will alter their
        // appearance.
        $(s".${prevOrNext}-blog-post").prop("disabled", true)
      } else {
        val title = titleVal.toString.toLowerCase.replace(" ", "-")
        // Redirect if desired
        if (redirect) {
          dom.window.location.href = s"${origin}/blog/${title}"
        }
      }
    }
  }
}
