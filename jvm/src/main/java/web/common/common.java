package com.ryanwhittingham.web.common;

import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class HtmlToPlainText {
  static String get(String html) {
    Document doc = Jsoup.parse(html);
    Elements paragraphs = doc.select("p");

    List<String> textList = new ArrayList<String>();
    for (Element p : paragraphs) {
      textList.add(p.ownText());
    }
    return String.join(" ", textList);
  }
}
