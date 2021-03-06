(defproject snigil "0.1.0-SNAPSHOT"
  :description "snigilbot: A quarto-playing bot taking its time"
  :url "https://github.com/hyPiRion/snigilbot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0-RC2"]
                 [org.clojure/tools.cli "0.2.2"]]
  :uberjar-name "snigil.jar"
  :profiles {:dev {:dependencies [[criterium "0.3.1"]]}}
  :main snigil.core)
