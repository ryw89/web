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
