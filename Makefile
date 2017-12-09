##-----------------------------------------------------------------------------
## Docker App
##-----------------------------------------------------------------------------

.DEFAULT_GOAL := help

##-----------------------------------------------------------------------------
## DEPENDENCIES
##-----------------------------------------------------------------------------

.PHONEY: init

CURL = curl -O -L --progress-bar

MAVEN = http://repo1.maven.org/maven2
CLOJARS = https://clojars.org/repo

LIB = lib

CLOJS =   $(LIB)/cljs.jar
ASYNC =   $(LIB)/core.async-0.3.465.jar
RUM =     $(LIB)/rum-0.10.8.jar
REACT =   $(LIB)/react-15.6.2-0.jar
RDOM =    $(LIB)/react-dom-15.6.2-0.jar
SABLONO = $(LIB)/sablono-0.7.7.jar

GET_CLOJS = https://github.com/clojure/clojurescript/releases/download/r1.9.473/cljs.jar
GET_ASYNC = $(MAVEN)/org/clojure/core.async/0.3.465/core.async-0.3.465.jar
GET_CLJS = $(MAVEN)/org/clojure/clojurescript/1.9.946/clojurescript-1.9.946.jar
GET_RUM = $(CLOJARS)/rum/rum/0.10.8/rum-0.10.8.jar
GET_REACT = $(CLOJARS)/cljsjs/react/15.6.2-0/react-15.6.2-0.jar
GET_RDOM = $(CLOJARS)/cljsjs/react-dom/15.6.2-0/react-dom-15.6.2-0.jar
GET_SABLONO = $(CLOJARS)/sablono/sablono/0.7.7/sablono-0.7.7.jar

$(CLOJS):
	@cd $(LIB) ; $(CURL) $(GET_CLOJS)

$(SABLONO):
	@cd $(LIB) ; $(CURL) $(GET_SABLONO)

$(RDOM):
	@cd $(LIB) ; $(CURL) $(GET_RDOM)

$(REACT):
	@cd $(LIB) ; $(CURL) $(GET_REACT)

$(RUM): $(REACT) $(RDOM) $(SABLONO)
	@cd $(LIB) ; $(CURL) $(GET_RUM)

$(ASYNC):
	@cd $(LIB) ; $(CURL) $(GET_ASYNC)

lib:
	@mkdir lib

init: lib $(CLOJS) $(ASYNC) $(RUM)  ## Pull down 3rd party library dependencies.

##-----------------------------------------------------------------------------

SRC = ./src

CLASSPATH = $(CLOJS):$(LIB)/*:$(SRC)
CLOJURE = java -cp $(CLASSPATH) clojure.main

TARGET = out

BUILD_FILE = ./bin/build.clj
WATCH_FILE = ./bin/watch.clj
REPL_FILE = ./bin/repl.clj
RELEASE_FILE = ./bin/release.clj

RLWRAP = $(shell which rlwrap)

.PHONY: clean dist-clean build watch repl release help run

clean: ## Remove build artifacts
	rm -rf $(TARGET)

dist-clean: clean ## Remove build artifacts including 3rd party libs
	rm -rf $(LIB)

build: init ## Build the client
	$(CLOJURE) $(BUILD_FILE)

watch: init ## Autobuild client when files change
	$(CLOJURE) $(WATCH_FILE) || true

repl: init ## Run a browser repl
	$(RLWRAP) $(CLOJURE) $(REPL_FILE)

release: init ## Create a release build.
	$(CLOJURE) $(RELEASE_FILE)

run: build ## Run a dev build.
	open index.html

run-release: clean release ## Run a release build
	open index.html

help: ## Produce this list of goals
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-25s\033[0m %s\n", $$1, $$2}' | \
		sort
