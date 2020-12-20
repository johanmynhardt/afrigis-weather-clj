(ns za.co.johanmynhardt.afrigis-weather-clj.core
  (:require [clojure.string :as str]
            [clojure.data.csv :as csv]
            [clj-http.client :as http]))

(def service-defaults
  {:service/host "https://saas.afrigis.co.za"
   :service/ns "weather"
   :service/base-path "/rest/v2"
   :service/auth-params "AUTH_PARAMS"
   :service/stations-csv "https://developers.afrigis.co.za/wp-content/uploads/2020/01/measurments_station.csv"

   :client/user-agent "afrigis-weather-clj"})

(def services
  [:measurements/getByCoord
   :measurements/getByBBox
   :measurements/getByStations

   :forecast.daily/getByCoord
   :forecast.daily/getByBBox
   :forecast.daily/getByStations

   :forecast.intraday/getByCoord
   :forecast.intraday/getByBBox
   :forecast.intraday/getByStations

   :tstorms/locationForecast.3
   :tstorms/locationHistory.3
   :tstorms/historicalDetails.3
   :tstorms/feed.3

   :radar/locations
   :radar/feed
   :radar/historicalDetails

   :lightning/locationHistory
   :lightning/historicalDetails
   :lightning/feed

   :alerts/feed
   :alerts/history]) (namespace :foo)

(defn service-url
  [{:service/keys [host base-path auth-params] service-ns :service/ns :as ctx}
   service-key]
  (str host base-path "/" service-ns
       (when (namespace service-key) (str "." (namespace service-key)))
       "." (name service-key)
       "/" auth-params "/"))

(defn api-request [{:client/keys [user-agent] :as ctx} service-key & [params]]
  (let [url (service-url ctx service-key)]
    (println "fetching " url)
    (http/get
     url
     {:query-params params
      #_#_:as :json
      :headers {:user-agent user-agent}})))

(defn fetch-stations []
  (let [stations-url (:service/stations-csv service-defaults)
        local-stations ; poor-man's cache this is...
        (try
          (slurp "stations.csv")
          (catch Throwable _
            (println "Fetching stations from" stations-url)
            (spit "stations.csv" (slurp stations-url))
            (slurp "stations.csv")))]
    (->> (csv/read-csv local-stations :separator \;)
         (drop 1)
         (map (fn [[synop_no station_name latitude longitude]]
                {:synop_no synop_no
                 :station_name station_name
                 :latitude latitude
                 :longitude longitude}))
         (sort-by :station_name))))

(defn search-stations [q]
  (->> (fetch-stations)
       (filter #(str/includes?
                 (str/lower-case (:station_name %))
                 (str/lower-case q)))))

(comment
  (->> (search-stations "bloemfontein")
       ;(map :station_name)
       println))

