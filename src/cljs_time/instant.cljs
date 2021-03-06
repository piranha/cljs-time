(ns cljs-time.instant
  (:require
   [goog.date.DateTime]
   [cljs-time.format :as fmt]))

(extend-protocol IPrintWithWriter
  goog.date.UtcDateTime
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (tf/unparse (:date-time tf/formatters) obj) writer opts))

  goog.date.DateTime
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (tf/unparse (:date-time tf/formatters) obj) writer opts))

  goog.date.Date
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (tf/unparse (:date tf/formatters) obj) writer opts)))
