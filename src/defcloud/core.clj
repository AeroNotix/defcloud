(ns defcloud.core
  (:require [com.stuartsierra.dependency :as dep]))


(def aws-registry (atom {}))
(def aws-dependencies (atom (dep/graph)))

(defn defthing [thing name opts]
  (let [options (apply hash-map opts)]
    `(do
       (let [vname# (-> ~name
                      quote
                      str
                      keyword)
             opt-map# (-> ~options
                        (conj [:kind ~thing])
                        (conj [:name vname#]))
             rvar# (def ~name opt-map#)]
         (mapv
           #(swap! aws-dependencies
              dep/depend vname# (:name %))
           (:deps opt-map#))
         (swap! aws-registry
           assoc vname# opt-map#)
         rvar#))))

(defmacro defawsmacro [norig]
  (let [n (symbol (str "def" (name norig)))]
    `(defmacro ~n [name# & opts#]
       (defthing ~norig name# opts#))))

(defawsmacro :elb)
(defawsmacro :asg)
(defawsmacro :group)
