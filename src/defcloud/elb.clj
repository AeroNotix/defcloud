(ns defcloud.elb
  (:require [amazonica.aws.elasticloadbalancing :as elb]
            [defcloud.core :as defcloud]
            [schema.core :as s])
  (:import [com.amazonaws.services.elasticloadbalancing.model
            LoadBalancerNotFoundException]))


(def ^:dynamic *default-availability-zones* ["us-east-1a"])
(def default-idle-timeout 3600)

(def listener-schema
  {:protocol (s/enum "tcp" "udp" "http")
   :load-balancer-port s/Int
   :instance-port s/Int})

(def az-schema [s/Str])

(def elb-schema
  {:name s/Str
   :listeners [listener-schema]
   :kind (s/enum :elb)
   (s/optional-key :deps) [s/Any]
   (s/optional-key :idle-timeout) s/Int
   (s/optional-key :availability-zones) az-schema})

(defn validate-listeners [listeners]
  (when-not (= (count (distinct (mapv :load-balancer-port listeners)))
              (count listeners))
    (throw (Exception. "Listeners cannot share the Load Balancer port"))))

(defmethod defcloud/validate :elb
  [elb]
  (s/validate elb-schema elb)
  (validate-listeners (:listeners elb)))

(defn schema->amazonica
  "Converts from the API schema to the Amazonica names."
  [{:keys [name availability-zones idle-timeout]
    :or {availability-zones *default-availability-zones*
         idle-timeout default-idle-timeout} :as all}]
  (merge all
    {:availability-zones availability-zones
     :load-balancer-attributes
     {:connection-settings
      {:idle-timeout idle-timeout}}
     :load-balancer-name name}))

(defn exists? [elb]
  (let [name (or (:name elb) elb)]
    (try
      (elb/describe-load-balancer-attributes
        :load-balancer-name name)
      (catch LoadBalancerNotFoundException e
        false))))

(defn update-elb-settings [elb existing]
  (let [elb-name    (:name elb)
        new-timeout (:idle-timeout elb)
        old-timeout (get-in existing [:connection-settings :idle-timeout])]
    (when-not (= new-timeout old-timeout)
      (elb/modify-load-balancer-attributes
        {:load-balancer-name elb-name
         :load-balancer-attributes
         {:connection-settings
          {:idle-timeout new-timeout}}}))))

(defn create-load-balancer [elb]
  (elb/create-load-balancer elb)
  (when (and (:idle-timeout elb)
          (not= (:idle-timeout elb)
            default-idle-timeout))
    (loop [existing? (exists? elb)]
      (if-not existing?
        (do
          (Thread/sleep 3000)
          (recur (exists? elb)))
        (update-elb-settings elb existing?)))))

(defmethod defcloud/create-in-aws! :elb
  [elb]
  (let [elb (->> elb
              (s/validate elb-schema)
              schema->amazonica)
        existing? (exists? elb)]
    (if existing?
      (update-elb-settings elb existing?)
      (create-load-balancer elb))))
