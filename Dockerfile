FROM clojure
COPY . /usr/src/lacity_council_crawl
WORKDIR /usr/src/lacity_council_crawl
EXPOSE 4242
CMD ["lein", "with-profile", "production", "run"]
