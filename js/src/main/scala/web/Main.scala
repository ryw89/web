package com.ryanwhittingham.web

import org.scalajs.dom

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
