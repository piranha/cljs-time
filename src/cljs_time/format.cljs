(ns cljs-time.format
  (:require
    [cljs-time.core :as time]
    [clojure.set :refer [difference]]
    [clojure.string :as string]
    [goog.date :as date]))

(def months
  ["January" "February" "March" "April" "May" "June" "July" "August"
   "September" "October" "November" "December"])

(def days
  ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"])

(def date-formatters
  (let [d      #(.getDate %)
        M #(inc (.getMonth %))
        y      #(.getYear %)
        h      #(.getHours %)
        m      #(.getMinutes %)
        s      #(.getSeconds %)
        S      #(.getMilliseconds %)
        ]
    {"d" d
     "dd" #(format "%02d" (d %))
     "dth" #(let [d (d %)] (str d (case d 1 "st" 2 "nd" 3 "rd" "th")))
     "dow" #(days (.getDay %))
     "EEE" #(days (.getDay %))
     "M" M
     "MM" #(format "%02d" (M %))
     "yyyy" y
     "yy" #(mod (y %) 100)
     "MMM" #(months (dec (M %)))
     "h" h
     "m" m
     "s" s
     "S" S
     "hh" #(format "%02d" (h %))
     "HH" #(format "%02d" (h %))
     "mm" #(format "%02d" (m %))
     "ss" #(format "%02d" (s %))
     "SSS" #(format "%03d" (S %))
     "Z" #(.getTimezoneOffsetString %)}))

(def date-parsers
  (let [y #(.setYear %1         (js/parseInt %2 10))
        d #(.setDate %1         (js/parseInt %2 10))
        M #(.setMonth %1   (dec (js/parseInt %2 10)))
        h #(.setHours %1        (js/parseInt %2 10))
        m #(.setMinutes %1      (js/parseInt %2 10))
        s #(.setSeconds %1      (js/parseInt %2 10))
        S #(.setMilliseconds %1 (js/parseInt %2 10))]
    {"d" ["(\\d{1,2})" d]
     "dd" ["(\\d{2})" d]
     "dth" ["(\\d{1,2})(?:st|nd|rd|th)" d]
     "M" ["(\\d{1,2})" M]
     "MM" ["((?:\\d{2})|(?:\\b\\d{1,2}\\b))" M]
     "y" ["(\\d{1,4})" y]
     "yy" ["(\\d{2,4})" y]
     "yyyy" ["(\\d{4})" y]
     "MMM" [(str \( (string/join \| months) \))
            #(M %1 (str (inc (.indexOf (into-array months) %2))))]
     "E" [(str \( (string/join \| (map #(string/join (take 3 %)) days)) \))
          (constantly nil)]
     "EEE" [(str \( (string/join \| days) \)) (constantly nil)]
     "dow" [(str \( (string/join \| days) \)) (constantly nil)]
     "m" ["(\\d{1,2})" m]
     "s" ["(\\d{1,2})" s]
     "S" ["(\\d{1,2})" S]
     "hh" ["(\\d{2})" h]
     "HH" ["(\\d{2})" h]
     "mm" ["(\\d{2})" m]
     "ss" ["(\\d{2})" s]
     "SSS" ["(\\d{3})" S]
     "Z" ["((?:\\+|\\-\\d{2}:\\d{2})|Z+)" (constantly nil)]}))


(defn parser-sort-order-pred [parser]
  (.indexOf
    (into-array ["yyyy" "yy" "y" "d" "dd" "dth" "M" "MM" "MMM" "dow" "h" "m"
                 "s" "S" "hh" "mm" "ss" "SSS" "Z"])
    parser))

(def date-format-pattern
  (re-pattern
    (str "(" (string/join ")|(" (reverse (sort-by count (keys date-formatters)))) ")")))

(defn date-parse-pattern [formatter]
  (re-pattern
    (string/replace (string/replace formatter #"'([^']+)'" "$1")
                    date-format-pattern
                    #(first (date-parsers %)))))

(defn formatter
  ([fmts]
   {:parser #(sort-by (comp parser-sort-order-pred second)
                      (partition 2
                                 (interleave
                                   (nfirst (re-seq (date-parse-pattern fmts) %))
                                   (map first (re-seq date-format-pattern fmts)))))
    :formatter (fn [date]
                 [(string/replace fmts #"'([^']+)'" "$1")
                  date-format-pattern
                  #((date-formatters %) date)])}))

(defn not-implemented [sym]
  #(throw (clj->js {:name :not-implemented
                    :message (format "%s not implemented yet" (name sym))})))

(def ^{:doc "Map of ISO 8601 and a single RFC 822 formatters that can be used
            for parsing and, in most cases, printing."}
  formatters
    {:basic-date (formatter "yyyyMMdd")
     :basic-date-time (formatter "yyyyMMdd'T'HHmmss.SSSZ")
     :basic-date-time-no-ms (formatter "yyyyMMdd'T'HHmmssZ")
     :basic-ordinal-date (formatter "yyyyDDD")
     :basic-ordinal-date-time (formatter "yyyyDDD'T'HHmmss.SSSZ")
     :basic-ordinal-date-time-no-ms (formatter "yyyyDDD'T'HHmmssZ")
     :basic-time (formatter "HHmmss.SSSZ")
     :basic-time-no-ms (formatter "HHmmssZ")
     :basic-t-time (formatter "'T'HHmmss.SSSZ")
     :basic-t-time-no-ms (formatter "'T'HHmmssZ")
     :basic-week-date (formatter "xxxx'W'wwe")
     :basic-week-date-time (formatter "xxxx'W'wwe'T'HHmmss.SSSZ")
     :basic-week-date-time-no-ms (formatter "xxxx'W'wwe'T'HHmmssZ")
     :date (formatter "yyyy-MM-dd")
     :date-element-parser (not-implemented 'dateElementParser)
     :date-hour (formatter "yyyy-MM-dd'T'HH")
     :date-hour-minute (formatter "yyyy-MM-dd'T'HH:mm")
     :date-hour-minute-second (formatter "yyyy-MM-dd'T'HH:mm:ss")
     :date-hour-minute-second-fraction (formatter "yyyy-MM-dd'T'HH:mm:ss.SSS")
     :date-hour-minute-second-ms (formatter "yyyy-MM-dd'T'HH:mm:ss.SSS")
     :date-opt-time (not-implemented 'dateOptionalTimeParser)
     :date-parser (not-implemented 'dateParser)
     :date-time (formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
     :date-time-no-ms (formatter "yyyy-MM-dd'T'HH:mm:ssZZ")
     :date-time-parser (not-implemented 'dateTimeParser)
     :hour (formatter "HH")
     :hour-minute (formatter "HH:mm")
     :hour-minute-second (formatter "HH:mm:ss")
     :hour-minute-second-fraction (formatter "HH:mm:ss.SSS")
     :hour-minute-second-ms (formatter "HH:mm:ss.SSS")
     :local-date-opt-time (not-implemented 'localDateOptionalTimeParser)
     :local-date (not-implemented 'localDateParser)
     :local-time (not-implemented 'localTimeParser)
     :ordinal-date (formatter "yyyy-DDD")
     :ordinal-date-time (formatter "yyyy-DDD'T'HH:mm:ss.SSSZZ")
     :ordinal-date-time-no-ms (formatter "yyyy-DDD'T'HH:mm:ssZZ")
     :time (formatter "HH:mm:ss.SSSZZ")
     :time-element-parser (not-implemented 'timeElementParser)
     :time-no-ms (formatter "HH:mm:ssZZ")
     :time-parser (formatter 'timeParser)
     :t-time (formatter "'T'HH:mm:ss.SSSZZ")
     :t-time-no-ms (formatter "'T'HH:mm:ssZZ")
     :week-date (formatter "xxxx-'W'ww-e")
     :week-date-time (formatter "xxxx-'W'ww-e'T'HH:mm:ss.SSSZZ")
     :week-date-time-no-ms (formatter "xxxx-'W'ww-e'T'HH:mm:ssZZ")
     :weekyear (formatter "xxxx")
     :weekyear-week (formatter "xxxx-'W'ww")
     :weekyear-week-day (formatter "xxxx-'W'ww-e")
     :year (formatter "yyyy")
     :year-month (formatter "yyyy-MM")
     :year-month-day (formatter "yyyy-MM-dd")
     :rfc822 (formatter "EEE, dd MMM yyyy HH:mm:ss Z")
     :mysql (formatter "yyyy-MM-dd HH:mm:ss")})


(def ^{:private true} parsers
  #{:date-element-parser :date-opt-time :date-parser :date-time-parser
    :local-date-opt-time :local-date :local-time :time-element-parser
    :time-parser})

(def ^{:private true} printers
  (difference (set (keys formatters)) parsers))

(defn parse
  "Returns a DateTime instance in the UTC time zone obtained by parsing the
  given string according to the given formatter."
  ([{:keys [parser]} s]
   (let [min-parts (count (string/split s #"(?:[^\w]+|'[^']+'|[TW])"))]
     (let [parse-seq (seq (map (fn [[a b]] [a (second (date-parsers b))])
                                  (parser s)))]
       (when (>= (count parse-seq) min-parts)
         (reduce (fn [date [part do-parse]] (do-parse date part) date)
                 (date/UtcDateTime. 0 0 0 0 0 0 0)
                 parse-seq)))))
  ([s]
   (first
     (for [f (vals formatters)
           :let [d (try (parse f s) (catch js/Error _))]
           :when d] d))))

(defn unparse
  "Returns a string representing the given DateTime instance in UTC and in the
form determined by the given formatter."
  [{:keys [formatter]} date]
  {:pre [(not (nil? date)) (instance? date/DateTime date)]}
  (apply string/replace (formatter date)))

(defprotocol Mappable
  (instant->map [instant] "Returns a map representation of the given instant.
                          It will contain the following keys: :years, :months,
                          :days, :hours, :minutes and :seconds."))

(defn- to-map [years months days hours minutes seconds millis]
  {:years years
   :months months
   :days days
   :hours hours
   :minutes minutes
   :seconds seconds
   :millis millis})

(extend-protocol Mappable
  goog.date.UtcDateTime
  (instant->map [dt]
    (to-map
      (.getYear dt)
      (inc (.getMonth dt))
      (.getDate dt)
      (.getHours dt)
      (.getMinutes dt)
      (.getSeconds dt)
      (.getMilliseconds dt))))

(extend-protocol Mappable
  ObjMap
  (instant->map [m]
    (case (:type (meta m))
      :cljs-time.core/period m
      :cljs-time.core/interval (time/->period m))))
