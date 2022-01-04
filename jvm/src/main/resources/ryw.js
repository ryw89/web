// Remove leading newlines in <pre><code> HTML blocks
let pre = document.getElementsByTagName("code");
for (let i = 0, len = pre.length; i < len; i++) {
  let text = pre[i].firstChild.nodeValue;
  pre[i].firstChild.nodeValue = text.replace(/^\n+|\n+$/g, "");
}

// Add search box handler
let searchInputElem = document.getElementById("blog-search-button");
searchInputElem.addEventListener("click", function () {
  ryw.searchHandler("blog-search-input");
});

// Allow for Enter key as well
$("#blog-search-input").keyup(function (event) {
  if (event.keyCode === 13) {
    $("#blog-search-button").click();
  }
});
