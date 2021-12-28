package com.ryanwhittingham.web.search

class Search(val query: String) {
  def queryIsValid(): Boolean = {
    if (query.length > 32) {
      false
    }
    true
  }
}
