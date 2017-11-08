# mount up

[mount](https://github.com/tolitius/mount) manages stateful components.

mount-up let's you know whenever any of these components are "managed".

[![Clojars Project](http://clojars.org/tolitius/mount-up/latest-version.svg)](http://clojars.org/tolitius/mount-up)

- [Ups and Downs](#ups-and-downs)
- [Listener](#listener)
  - [Logging](#logging)
- [Listening to Ups and Downs](#listening-to-ups-and-downs)
  - [On up](#on-up)
  - [Removing all the listeners](#removing-all-the-listeners)
  - [On up and down](#on-up-and-down)
- [Wrapping](#wrapping)
  - [Exception Handling](#exception-handling)
- [License](#license)

## Ups and Downs

There are three types of events you can listen to:

Whenever any state / component is

* started

```clojure
(on-up [k f when])
```

* stopped

```clojure
(on-down [k f when])
```

* started and/or stopped

```clojure
(on-upndown [k f when])
```

where:

`k`: key / name of the listner<br/>
`f`: function / listener<br/>
`when`: when to apply `f`. possible values `:before`, `:after` or `:wrap-in`

## Listener

As anything good in Clojure, listener is just a function.

This function will be passed a map with `:name` and `:action` keys.

`:name` will have a component's name<br/>
`:action` will have an action taked: i.e. `:up` or `:down`

### Logging

mount-up comes with one such listener that logs whenever any of the states / components are started or stopped:

```clojure
(defn log [{:keys [name action]}]
  (case action
    :up (info ">> starting.." name)
    :down (info "<< stopping.." name)))
```

## Listening to Ups and Downs

Let's use the `log` function above as an example.

```clojure
$ boot dev
```

Creating a server component, starting it and stopping it as usual:

```clojure
boot.user=> (require '[mount.core :as mount :refer [defstate]])
nil
boot.user=> (defstate server :start 42 :stop -42)
#'boot.user/server

boot.user=> (mount/start)
{:started ["#'boot.user/server"]}

boot.user=> (mount/stop)
{:stopped ["#'boot.user/server"]}
```

### On up

Now let's listen whenever this component is started and log _:before_ it happens:

```clojure
boot.user=> (require '[mount-up.core :as mu])
nil
boot.user=> (mu/on-up :info mu/log :before)
{:info #object[clojure.core$partial$fn__4761 0x703ef68c "clojure.core$partial$fn__4761@703ef68c"]}

boot.user=> (mount/start)
INFO  mount-up.core - >> starting.. #'boot.user/server
{:started ["#'boot.user/server"]}

boot.user=> (mount/stop)
{:stopped ["#'boot.user/server"]}
```

### Removing all the listeners

We can also clear all the listeners by `all-clear`:

```clojure
boot.user=> (mu/all-clear)
nil
boot.user=> (mount/start)
{:started ["#'boot.user/server"]}
boot.user=> (mount/stop)
{:stopped ["#'boot.user/server"]}
```

### On up and down

```clojure
boot.user=> (mu/on-upndown :info mu/log :before)
{:info #object[clojure.core$partial$fn__4761 0x75fda4b5 "clojure.core$partial$fn__4761@75fda4b5"]}

boot.user=> (mount/start)
INFO  mount-up.core - >> starting.. #'boot.user/server
{:started ["#'boot.user/server"]}

boot.user=> (mount/stop)
INFO  mount-up.core - << stopping.. #'boot.user/server
{:stopped ["#'boot.user/server"]}
```

`mu/log` function is just an example of course: any function(s) can be used as a listener.

## Wrapping

Besides `:before` and `:after`, mount-up knows how to _wrap_ ups and downs with a custom function via `:wrap-in`.

This is really useful in case you need to be in charge of calling start or stop for each individual state.
For example to guard ups and downs of each state with a `try/catch`.

A "wrapper" function takes two arguments:

`f`: a function that is going to bring state up or down<br/>
`state-name`: a name of the state (i.e. `"#'app/db"`)<br/>

Function `f` will be provided by mount and will just need to be invoked as `(f)` to start/stop the state. The rest is up to you.

### Exception Handling

It is a lot simpler to demo than to explain.

mount-up comes with a generic `try-catch` function:

```clojure
(defn try-catch [on-error]
  (fn [f state]
    (try (f)
         (catch Throwable t
           (on-error t state)))))
```

which returns a function that takes `f` and `state` (name) and wraps calling `(f)` in a `try/catch`. It takes an `on-error` function
that will decide what will happen if starting or stopping state results in a `Throwable`.

Let's define a sample `on-error` function that will eat (ouch!) the exception and will log what happened:

```clojure
boot.user=> (defn log-exception [ex _]
              (let [root (.getMessage (.getCause ex))]
                (log/error (str (.getMessage ex) " \"" root \"))))
#'boot.user/log-exception
```

Let's define three states, one of which throws an exception:

```clojure
boot.user=> (defstate server :start 42 :stop -42)
#'boot.user/server
boot.user=> (defstate db :start (/ 1 0) :stop -42)
#'boot.user/db
boot.user=> (defstate pi :start 3.14 :stop 14.3)
#'boot.user/pi
```

Let's start these without wrapping anything:

```clojure
boot.user=> (mount/start)
INFO  mount-up.core - >> starting.. #'boot.user/server
INFO  mount-up.core - >> starting.. #'boot.user/db

java.lang.ArithmeticException: Divide by zero
   java.lang.RuntimeException: could not start [#'boot.user/db] due to
```

As expected `#'boot.user/db` throws an exception and we have no control over it. Also notice that system failed
(which in most cases is the right behavior), so `#'boot.user/pi` was not even attempted to start.

Let's plug in a sample `try-catcher` "on-up" and see what it does:

```clojure
boot.user=> (mu/on-up :guard (mu/try-catch log-exception) :wrap-in)
{:guard
 #object[clojure.core$partial$fn__4761 0x7fbb46f2 "clojure.core$partial$fn__4761@7fbb46f2"],
 :info
 #object[clojure.core$partial$fn__4761 0x656ab49 "clojure.core$partial$fn__4761@656ab49"]}
```

(we still have the `:info` logger from the above section to help with a visual)

Notice the `:wrap-in` instead of `:after` or `:before`.

Let's stop and start it again:

```clojure
boot.user=> (mount/stop)
{:stopped ["#'boot.user/server"]}
```

```clojure
boot.user=> (mount/start)
INFO  mount-up.core - >> starting.. #'boot.user/server
INFO  mount-up.core - >> starting.. #'boot.user/db
ERROR boot.user - could not start [#'boot.user/db] due to "Divide by zero"
INFO  mount-up.core - >> starting.. #'boot.user/pi
{:started ["#'boot.user/server" "#'boot.user/pi"]}
```

this time we "controlled" the exception, reported the problem and _decided_ the system may start without a database.

Let's check what all these state look like:

```clojure
boot.user=> (require '[mount.tools.graph :as graph])
```
```clojure
boot.user=> (graph/states-with-deps)
({:name "#'boot.user/server", :order 1, :status #{:started}, :deps #{}}
 {:name "#'boot.user/db", :order 2, :status #{:stopped}, :deps #{}}
 {:name "#'boot.user/pi", :order 3, :status #{:started}, :deps #{}})
```

again, a built in `try-catch` is just an example of a custom wrapper function.

## License

Copyright © 2017 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
