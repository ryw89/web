package com.ryanwhittingham.web.errors

object ServeError {
  def serve(code: Int): cask.Response[String] = {
    code match {
      case 404 => cask.Response("404", statusCode = 404)
      case _   => cask.Response("500", statusCode = 500)
    }
  }
}
