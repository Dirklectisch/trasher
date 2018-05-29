(defproject trasher "0.1.0-SNAPSHOT"
  :description "A small script to clean up my downloads folder"
  :license {:name "MIT License"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :main trasher.core
  :profiles {:uberjar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :aot :all}})
