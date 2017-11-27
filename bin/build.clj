(require 'cljs.build.api)

(def params
  {:output-to "out/main.js"
   :main 'docker_app.main})

(cljs.build.api/build "src" params)
