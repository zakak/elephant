(ns clojurewerkz.elephant.customers
  (:refer-clojure :exclude [list update])
  (:require [clojurewerkz.elephant.conversion    :as cnv]
            [clojurewerkz.elephant.subscriptions :as sub]
            [clojurewerkz.elephant.util :refer (api-key->request-options)]
            [clojure.walk :as wlk])
  (:import [clojure.lang IPersistentMap]
           [com.stripe.model Customer]))

;;
;; API
;;

(defn ^IPersistentMap create
  [^IPersistentMap m]
  (cnv/customer->map (Customer/create m)))

(defn ^IPersistentMap retrieve
  [^String id]
  (cnv/customer->map (Customer/retrieve id)))

(defn ^IPersistentMap retrieve-or-create
  [^String id ^IPersistentMap m]
  (try
    (retrieve id)
    (catch com.stripe.exception.InvalidRequestException ire
      (cnv/customer->map (Customer/create m)))))

(defn ^IPersistentMap subscribe
  [^IPersistentMap customer ^IPersistentMap subscription]
  (sub/create customer subscription))

(defn cancel-all-subscriptions
  [^IPersistentMap customer]
  (some->> customer
           :subscriptions
           (map sub/cancel)
           doall))

(defn ^IPersistentMap update
  [^IPersistentMap customer ^IPersistentMap m]
  (if-let [o (:__origin__ customer)]
    (cnv/customer->map (.update o m))
    (throw (IllegalArgumentException.
            "customers/update only accepts maps returned by customers/create, charges/retrieve, and customers/list"))))

(defn ^IPersistentMap update-default-source
  [^IPersistentMap customer ^String id]
  (update customer {"default_source" id}))

(defn ^IPersistentMap apply-coupon
  [^IPersistentMap customer ^String coupon]
  (update customer {"coupon" coupon}))

(defn list
  ([]
     (list {}))
  ([m]
     (cnv/customers-coll->seq (Customer/list (wlk/stringify-keys m))))
  ([^String api-key m]
     (cnv/customers-coll->seq (Customer/list (wlk/stringify-keys m) (api-key->request-options api-key)))))

(defn ^IPersistentMap delete
  ([m]
     (if-let [o (:__origin__ m)]
       (cnv/deleted-customer->map (.delete o))
       (throw (IllegalArgumentException.
               "customers/delete only accepts maps returned by customers/create, charges/retrieve, and customers/list"))))
  ([m ^String api-key]
     (if-let [o (:__origin__ m)]
       (cnv/deleted-customer->map (.delete o api-key))
       (throw (IllegalArgumentException.
               "customers/delete only accepts maps returned by customers/create, charges/retrieve, and customers/list")))))
