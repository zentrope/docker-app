# Docker • App

Presents an extremely simple (but illustrative) web app for monitoring
docker status on a localhost of your choosing.


**NOTE:** This is a MacOS centric guide. None of those will work on
Windows, and you _might_ have to make a few changes for Linux.

## Prerequisites

* Docker
* Java 8 (Open JDK)
* Internet connection (for pulling dependencies)
* browser (Safari, Firefox, Chrome)
* [docker-proxy][prox] utility
* A command line that supports "open index.html" (i.e., at least
  MacOS).
* git
* make

## Method

Use a client/server architecture (this project being the client) to
to query the [Docker REST API][dapi] via typical
[fetch][ftch] requests.

The app will display (and poll for changes of) the following
information:

* API version info → `docker version`
* Containers → `docker ps`
* Images → `docker images`
* Networks → `docker network ls`
* Volumes → `docker volume ls`

The app will poll every 12 seconds or so for changes.

This project is the "client", and [docker-proxy][prox] is the server.

[dapi]: https://docs.docker.com/engine/api/latest/
[ftch]: https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
[prox]: https://github.com/zentrope/tools/tree/master/cmd/docker-proxy

## Server: docker-proxy

**Problem:** On the Mac, when using the Docker For Mac application,
you can't access the Docker REST API via a network socket listening on
the loopback interface. I don't see how you can change that without
surgery in the installed App. The Homebrew version isn't turn key,
doesn't use the hypervisor by default, etc, etc. You're left with
using the [Unix domain socket][usck].

[usck]: https://en.wikipedia.org/wiki/Unix_domain_socket

**Solution:** Use a service to proxy **from another project** from web
requests to a UNIX domain sockets.

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

**TIP -- to spy on how the docker client translates commands to requests:**

_This is not required to run the web app._

Run this in your `bash` shell:

    $ export DOCKER_HOST="tcp://localhost:2375"

or `fish`:

    $ set gx DOCKER_HOST tcp://localhost:2375

When you issue standard `docker` commands in that same shell,
`docker-proxy` shows how those commands translate into docker REST API
requests.

This is especially handy for figuring out how the various `--filter`
commands translate.

## Client: build &amp; run the app

First, check out this repo:

    $ git clone git@github.com:zentrope/docker-app.git

then try:

    $ cd docker-app
    $ make run

to build and run the app. The make system will pull down some
dependencies. As long as you're connected to the Internet, everything
should Just Work.

## Implementation side-note

This app is written in ClojureScript mainly so that I could revisit
the tech and to see how simple I could make the build.
