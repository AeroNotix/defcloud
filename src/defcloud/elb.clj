(ns defcloud.elb
  (:require [defcloud.core :as defcloud]
            [schema.core :as s]))


(def ^:dynamic *default-availability-zones* ["us-east-1a"])

(def listener-schema
  {:protocol (s/enum "tcp" "udp")
   :load-balancer-port s/Int
   :instance-port s/Int})

(def az-schema [s/Str])

(def elb-schema
  {:name s/Str
   :listeners [listener-schema]
   :kind (s/enum :elb)
   (s/optional-key :availability-zones) az-schema})

(defmethod defcloud/validate :elb
  [elb]
  (s/validate elb-schema elb))

(defmethod defcloud/create-in-aws! :elb
  [elb]
  true)
