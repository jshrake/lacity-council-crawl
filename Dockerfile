FROM clojure
COPY . /usr/src/lacity_council_crawl
WORKDIR /usr/src/lacity_council_crawl
CMD ["lein", "with-profile", "production", "run"]
