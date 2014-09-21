(require 'generic-x)
(define-generic-mode 'peg4d-mode
  ;;comment
  '(
    "//"
    ("/\\*" . "\\*/")
   )
  ;;keyward
  nil
  ;;other
  '(
    ("\sw" . 'font-lock-string-face)
    ("'.*?'" . 'font-lock-string-face)
    ("`.*?`" . 'font-lock-string-face)
    ("{" . 'font-lock-function-name-face)
    ("}" . 'font-lock-function-name-face)
    ("@" . 'font-lock-function-name-face)
    ("#[A-Za-z0-9_]+" . 'font-lock-builtin-face)
    ("^[A-Za-z0-9_]+" . 'font-lock-keyword-face)
   )
  ;;file
  nil
  ;;
  nil
  ;;
  "mode for peg4d"
)
(setq auto-mode-alist
      (append '(("\\.peg$" . peg4d-mode)
                ("\\.p4d$" . peg4d-mode)) auto-mode-alist)
)