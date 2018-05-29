(ns trasher.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.string :refer [split-lines]])
  (:import (java.lang System)
           (java.util Date)
           (java.text SimpleDateFormat)
           (java.text ParsePosition))
  (:gen-class))

(set! *warn-on-reflection* true)

(def downloads-path-name
  (str (System/getProperty "user.home") "/Downloads"))

(defn is-empty-folder?
  [path-name]
  (empty? (.list (io/file path-name))))

(defn sh-bash
  [cmd]
  (:out (sh "bash" "-c" cmd)))

(def spotlight-names
  {"kMDItemFSName" ::file-name
   "kMDItemContentType" ::file-type
   "kMDItemDateAdded" ::date-added
   "kMDItemLastUsedDate" ::date-opened})

(def spotlight-attributes
  (keys spotlight-names))

(def date-formatter
  (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss Z"))

(defn parse-spotlight-date
  [string]
  (.parse ^SimpleDateFormat date-formatter string (ParsePosition. 0)))

(defn parse-spotlight-value
  [string]
  (cond
    (= "(null)" string) nil
    (re-matches #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} \+\d{4}" string) (parse-spotlight-date string)
    :default
      (let [[_ inner] (re-matches #"\"(.+?)\"" string)]
        inner)))

(defn parse-spotlight-out
  [string attribute-count target-dir]
  (if (empty? string)
    []
    (->> (split-lines string)
         (partition attribute-count)
         (map (fn [line-group]
                (->> line-group
                     (map (fn [line]
                            (let [[_ name-str value-str] (re-matches #"(\w+)\s+=\s(.+)" line)
                                  key (spotlight-names name-str)
                                  value (parse-spotlight-value value-str)]
                              {key value})))
                     (apply merge))))
         (map (fn [{:keys [::file-type] :as file-meta}]
                (let [file-path (str target-dir "/" (::file-name file-meta))]
                  (merge file-meta
                         {::file-path file-path}
                         (if (= file-type "public.folder")
                           {::is-empty? (is-empty-folder? file-path)}
                           {}))))))))

(defn get-spotlight-metadata
  [attributes target-dir]
  (let [cmd-names (apply str (interpose " -name " attributes))
        cmd (apply str (interpose " " ["mdls -name" cmd-names (str target-dir "/*")]))
        files (parse-spotlight-out (sh-bash cmd) (count attributes) target-dir)
        folders (filter #(= "public.folder" (::file-type %)) files)
        sub-files (mapcat #(get-spotlight-metadata attributes (::file-path %)) folders)]
    (into files sub-files)))

(def year-ago-millis
  (- (.getTime (Date.)) (* 365 24 60 60 1000)))

(defn is-old-file?
  [file-meta]
  (let [{:keys [::date-added ::date-opened ::is-empty?]} file-meta]
    (if is-empty?
      true
      (and
        (< (.getTime ^Date date-added) year-ago-millis)
        (or
          (= date-opened nil)
          (< (.getTime ^Date date-opened) year-ago-millis))))))

(defn filter-old-files
  [file-metas]
  (filterv is-old-file? file-metas))

(defn find-old-files
  [target-dir]
  (-> (get-spotlight-metadata spotlight-attributes target-dir)
      (filter-old-files)))

(defn move-to-trash
  [source-path]
  (sh-bash (str "osascript -e 'tell app \"Finder\" to move the POSIX file \"" source-path "\"to trash'")))

(defn -main
  []
  (doseq [{:keys [::file-path]} (find-old-files downloads-path-name)]
    (println file-path))
  (shutdown-agents))

(comment
  (user/refresh)
  (-main))
