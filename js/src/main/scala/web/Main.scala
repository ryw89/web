package com.ryanwhittingham.web

import org.scalajs.dom

import scalajs.js
import scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel(name = "ryw") @JSExportAll
object Main {

  /** Correct links with a certain class by making them relative to the
    * main website's origin.
    *
    * In HTML, 'href' attributes are of course relative to the current
    * page; e.g. an href of "whatever" on example.com/whatever will
    * yield example.com/whatever/whatever when followed.
    *
    * This function fixes, in an ad-hoc way, such undesired relative
    * links by "resetting" them to come from the origin; i.e.
    * example.com/whatever/whatever will become example.com/whatever.
    *
    * This functionality relies on a DOM element's "attributes.href.value"
    * property as well as the value of window.location.origin.
    */
  def makeHrefRelativeToOrigin(elemClass: String): Unit = {
    val document = dom.document
    val window = dom.window

    // Will be something like "http://localhost or https://example.com"
    val origin = window.location.origin

    val elems = document.getElementsByClassName(elemClass)
    val elemsCount = elems.length

    for (i <- 0 until elemsCount) {
      val elem = elems(i)

      // rawHref -- i.e., attributes.href.value -- will be something
      // like "blog/by-month/2021-10". I believe it should not contain
      // undesired relative paths.
      val rawHref =
        elem.attributes.getNamedItem("href").value
      val newHref = s"${origin}/${rawHref}"
      elem.setAttribute("href", newHref)
    }
  }

  /** Custom handler for main search bar. Intended to be attached to a
    * mouse click handler for the search button.
    */
  def searchHandler(formInputId: String): Unit = {
    val document = dom.document
    val window = dom.window
    val formValue = document.getElementById(formInputId).getAttribute("value")

    // Validate search contents
    if (formValue.length > 32) {
      window.alert("Query is too long.")
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
}
