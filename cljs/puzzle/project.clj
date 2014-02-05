(defproject pcg "1.0"
  :description "lichess.org puzzle solver"
  :source-paths ["src-clj"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156" :exclusions [org.apache.ant/ant]]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [prismatic/dommy "0.1.2"]
                 [cljs-ajax "0.2.3"]]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :cljsbuild {
              :builds {
                       :dev {
                             :source-paths ["src"]
                             :compiler {:output-to "../../public/compiled/puzzle.js"
                                        ; :source-map "../../public/compiled/puzzle.js.map"
                                        ; :output-dir "../../public/compiled/target"
                                        :optimizations :whitespace
                                        :pretty-print true}}
                       :prod {
                              :source-paths ["src"]
                              :compiler {:output-to "../../public/compiled/puzzle.js"
                                         :externs ["externs.js"]
                                         :optimizations :advanced
                                         :pretty-print false}}}})
