# Ring-Serve

Ring-Serve provides a convenient way of starting a web server for a
Ring handler.

## Features

The `serve` macro:

* Is safe to run multiple times.
* Finds a free port if you do not specify one.
* Opens up a web browser to view your handler, or if you don't want
  this, call the `serve-headless` macro instead.
* Automatically takes into account any changes made to your
  handler.
* Wraps your handler in the `wrap-stacktrace` middleware.
* Ensures that the `swank.core/break` function works correctly inside
  your handler.


## Example

    user> (defn handler [request]
            {:status 200
             :headers {"Content-Type" "text/plain"}
             :body "Hello World"})
    #'user/handler
    user> (use 'ring.util.serve)
    nil
    user> (serve handler)
    Started web server on port 3000
    nil
    user> (stop-server)
    Stopped web server
    nil

## Installation

Include the following dependency in your Leiningen project file:

    [ring-serve "0.1.1"]
