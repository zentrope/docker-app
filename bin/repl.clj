(require 'cljs.repl)
(require 'cljs.build.api)
(require 'cljs.repl.browser)

(def repl-build-params
  {:main 'docker-app.main
   :output-to "out/main.js"
   :browser-repl true
   :verbose true})

(cljs.build.api/build "src" repl-build-params)
(cljs.repl/repl (cljs.repl.browser/repl-env)
                :watch "src"
                :output-dir "out")
