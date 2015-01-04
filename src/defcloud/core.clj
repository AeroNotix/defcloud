(ns defcloud.core
  (:require [com.stuartsierra.dependency :as dep]))


(def aws-registry (atom {}))
(def aws-dependencies (atom (dep/graph)))

(defmulti validate :kind)
(defmulti create-in-aws! :kind)

(defn defthing [thing name opts]
  (let [options (apply hash-map opts)]
    `(do
       (let [vname# (-> ~name
                      quote
                      str)
             opt-map# (-> ~options
                        (conj [:kind ~thing])
                        (conj [:name vname#]))
             rvar# (def ~name opt-map#)]
         (validate opt-map#)
         (mapv
           #(swap! aws-dependencies
              dep/depend vname# (:name %))
           (if (seq (:deps opt-map#))
             (:deps opt-map#)
             [{:name :base}]))
         (swap! aws-registry
           assoc vname# opt-map#)
         rvar#))))

(defmacro defawsmacro [name-original]
  (let [n (symbol (str "def" (name name-original)))]
    `(defmacro ~n [name# & opts#]
       (defthing ~name-original name# opts#))))

(defawsmacro :elb)
(defawsmacro :asg)
(defawsmacro :group)
