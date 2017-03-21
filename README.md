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
`when`: when to apply `f`. possible values `:before` or `:after`

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
