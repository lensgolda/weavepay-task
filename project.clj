(defproject weavepay-task "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.773"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [org.clojure/data.json "1.1.0"]
                 [thheller/shadow-cljs "2.11.21"]
                 [reagent "1.0.0"]
                 [re-frame "1.2.0"]
                 [day8.re-frame/http-fx "0.2.2"]
                 [re-com "2.13.2"]
                 [yogthos/config "1.1.7"]
                 [ring "1.9.1"]
                 [org.slf4j/slf4j-api "1.7.30"]
                 [org.slf4j/slf4j-simple "1.7.30"]
                 [io.pedestal/pedestal.service "0.5.8"
                  :exclusions [ch.qos.logback/logback-classic]]
                 [io.pedestal/pedestal.service-tools "0.5.8"
                  :exclusions [ch.qos.logback/logback-classic]]
                 [io.pedestal/pedestal.route "0.5.8"]
                 [io.pedestal/pedestal.jetty "0.5.8"]
                 [org.martinklepsch/clj-http-lite "0.4.3"]
                 [org.xerial/sqlite-jdbc "3.34.0"]
                 [com.github.seancorfield/next.jdbc "1.1.643"]
                 [com.zaxxer/HikariCP "4.0.3"]]

  :plugins [[lein-shadow "0.3.1"]
            [lein-shell "0.5.0"]]

  :min-lein-version "2.9.0"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]


  :shadow-cljs {:nrepl {:port 8777}

                :builds {:app {:target :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules {:app {:init-fn weavepay-task.core/init
                                               :preloads [devtools.preload]}}

                               :devtools {:http-root "resources/public"
                                          :http-port 8280
                                          :http-handler weavepay-task.handler/dev-handler}}}}


  :shell {:commands {"karma" {:windows         ["cmd" "/c" "karma"]
                              :default-command "karma"}
                     "open"  {:windows         ["cmd" "/c" "start"]
                              :macosx          "open"
                              :linux           "xdg-open"}}}

  :aliases {"dev"          ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein watch instead.\""]
                            ["watch"]]
            "watch"        ["with-profile" "dev" "do"
                            ["shadow" "watch" "app" "browser-test" "karma-test"]]

            "prod"         ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein release instead.\""]
                            ["release"]]

            "release"      ["with-profile" "prod" "do"
                            ["shadow" "release" "app"]]

            "build-report" ["with-profile" "prod" "do"
                            ["shadow" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]

            "karma"        ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein ci instead.\""]
                            ["ci"]]
            "ci"           ["with-profile" "prod" "do"
                            ["shadow" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "1.0.2"]]
    :source-paths ["dev"]
    :jvm-opts ["-Dclojure.server.repl={:address,\"0.0.0.0\",:port,50505,:accept,clojure.core.server/repl}"
               "-Dorg.slf4j.simpleLogger.defaultLogLevel=info"]}

   :prod {}

   :uberjar {:source-paths ["env/prod/clj"]
             :omit-source  true
             :main         weavepay-task.server
             :aot          [weavepay-task.server]
             :uberjar-name "weavepay-task.jar"
             :prep-tasks   ["compile" ["release"]]}}

  :prep-tasks [])
