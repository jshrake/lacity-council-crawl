# lacity-council-crawl

A crawler and scraper for [LA city council voting data](http://citycouncil.lacity.org/cvvs/search/search.cfm) built with Clojure

## Usage

This service crawls votes [10 seconds at a time](http://lacity.org/robots.txt). As of this commit, there are roughly ~80,000 votes in the system.
Once this service has crawled all existing votes, it will attempt to retrieve newly added votes indefintely.

### Dev

Stores the data in an SQLite database under `db`:

```bash
mkdir db
lein run
```

### Production

Create a production configuration file at `resources/production_config.edn` that specifies the database information. You can model it after [resources/dev_config.edn](resources/dev_config.edn).

```bash
lein with-profile production run
```

## Deploy

As this is a service that should run indefinitely, it may be desriable to deploy it to the great big cloud in the sky. I've chosen to deploy to a free instance of Amazon's [Elastic Beanstalk](https://aws.amazon.com/elasticbeanstalk/):

0. Create a production configuration file
1. Install the Amazon EB CLI `brew install awsebcli`
2. `eb init`
3. `eb create`
4. `eb deploy`

## License

MIT
