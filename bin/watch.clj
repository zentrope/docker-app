(require 'cljs.build.api)

(def watch-params
  {:main 'docker-app.main
   :verbose false
   :output-to "out/main.js"})

(cljs.build.api/watch "src" watch-params)
