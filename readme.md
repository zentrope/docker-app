# Docker • App

Presents an extremely simple (but illustrative) web app for monitoring
docker status on a localhost of your choosing.


**NOTE:** This is a MacOS centric guide. None of those will work on
Windows, and I don't have a Linux instance to test with.

## Requirements

* Docker
* Java 8 (Open JDK)
* Internet connection (for pulling dependencies)
* Browser
* Docker Proxy utility
* A command line that supports "open index.html" (i.e., at least
  MacOS).

## Method

Create a web-app to query the [Docker REST API][dapi] via typical
[fetch][ftch] requests.

The app will display (and poll for changes of) the following
information:

* API version info → `docker version`
* Containers → `docker ps`
* Images → `docker images`
* Networks → `docker network ls`
* Volumes → `docker volume ls`

The app will poll every 12 seconds or so for changes.

[dapi]: https://docs.docker.com/engine/api/latest/
[ftch]: https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch

## Docker Proxy

**Problem:** On the Mac, when using the Docker For Mac application,
you can't access the Docker REST API via a network socket listening on
the loopback interface, and I don't see how you can change that
without surgery. The Homebrew version isn't turn key, doesn't use the
hypervisor by default, etc, etc. You're left with using the [Unix
domain socket][usck].

[usck]: https://en.wikipedia.org/wiki/Unix_domain_socket

**Solution:** Use a service to proxy from web requests to a UNIX
domain sockets.

Assuming you have Go installed:

    $ brew install go

and you've set up a `$GOPATH`, then you can do the following:

    $ go get -u github.com/zentrope/tools/cmd/docker-proxy

which should install the `docker-proxy` command in `$GOPATH\bin` which
is hopefully on your regular path. You can keep using the above
command with the `-u` switch to get updates to the command if it ever
changes.

Then:

    $ docker-proxy

and you'll be able to make web requests to `http://localhost:2375` and
get back JSON data.

**TIP**

Run this in your `bash` shell:

    $ export DOCKER_HOST="tcp://localhost:2375"

or `fish`:

    $ set gx DOCKER_HOST tcp://localhost:2375

When you issue standard `docker` commands in that same shell,
`docker-proxy` shows how those commands translate into docker REST API
requests.

This is especially handy for figuring out how the various `--filter`
commands translate.

## Build/Run the App

Try:

    $ make run

to build and run the app. The make system will pull down some
dependencies. As long as you're connected to the Internet, everything
thing Just Work.

## Implementation Thing

This app is written in ClojureScript mainly so that I could revisit
the tech, and to see if I could get it to work without using a Clojure
build and dependency management system.

The experiment has taught me that I can use Make (or shell scripts) to
make this project work (at least on Unix) and that ClojureScript is
simpler than the current `create-react-app` or `webpack` toolchain.
