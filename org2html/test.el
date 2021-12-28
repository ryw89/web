(load-file "/root/.emacs.d/lisp/emacs-htmlize/htmlize.el")

(find-file "/root/test.org")
(org-html-export-to-html)
