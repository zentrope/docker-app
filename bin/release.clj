(require '[cljs.build.api :refer [build]])

(build "src"
       {:output-to "out/main.js"
        :optimizations :advanced})

(System/exit 0)
