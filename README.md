# defcloud

A Clojure DSL for describing EC2 infrastructure

## Example

```clojure
(ns yourns.core
  (:require [defcloud.core :only [defelb create-all-in-aws!]]))

(let [listener-5555 {:protocol "tcp"
                     :load-balancer-port 5555
                     :instance-port 5555}]

  (defelb foo
    :listeners
    [listener-5555])

  (defelb foo2
    :deps [foo] ;; Defines the creation order
    :idle-timeout 50
    :listeners
    [listener-5555
    (assoc listener-5555 :protocol "http")]))

(create-all-in-aws!)
```

## License

Copyright © 2015 Aaron France

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
