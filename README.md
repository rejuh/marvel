# marvel

A task was given to use data provided in https://www.kaggle.com/dannielr/marvel-superheroes#marvel_dc_characters.csv

The task description is as follows:
Develop a solution that seeks to join related data together into document-based data structure, saving this to a suitable format (e.g. JSON, AVRO, Parquet) and load into a database of your choice (SQL/NoSQL/SparkSQL/Presto) hosted on Docker.  You may use any programming language of choice for ETL. You will be required to discuss and answer questions about your solution, approach and challenges faced.

I have chosen to have a pipeline running in Docker, which uses Spark (scala) to load data from csvs in an HDFS location, and writes parquets to a another HDFS location. It should also load data from parquets from a HDFS location and write to a new table in Hive

#Deployment
The setup requires outside setup, new docker containers should be launched to host the Hive database, HDFS etc. And our marvel files uploaded to HDFS.
The package is available in https://hub.docker.com/r/bde2020/hive. a "docker-compose up -d" can launch Hive in the docker-hive_default network.

For this project, "docker-compose up -d" lanuches a spark-master and 2 spark-workers in the docker-hive network. And spark jobs will have to be started separately.
To build a spark job run the following command: "docker build --rm=true -t bde/spark-app ."
To lanuch the spark job run: "docker run --name my-spark-app -e ENABLE_INIT_DAEMON=false --link spark-master:spark-master --net docker-hive_default -d bde/spark-app"
This launches a batch spark job against our spark-master in the docker-hive_default network. This allows communication against the HDFS and Hive locations which are required for the spark operations
This job will last as long as processing is still being executed and will exit when processing completes.
  
# Decisions 
Due to lack of information assumptions had to be made about data, access, methods of processing and and launching. 

It is assumed a user may want to access all data relevant to data points where available.
Joins were designed to be as all inclusive as possible. This means a full outer join. Smaller datasets were broadcast for performance, and "Name" is used as the most common key, hence the data is co-located using "Name".
Queries can be done against hive to look at data from any view as all possible related data is connected.
For marvel_dc_characters.csv and marvel_characters_info.csv these files have similar schemas (with a few different columns) and distinct keys for ID. 
Hence a separate join is done to merge these files separately to a single Dataframe.

There are also boolean columns, and integer columns in superheroes_power_matrix.csv and charcters_stats.csv so columns were renamed to avoid datatyping issues.

A final transformation to replace characters is used to satisfy requirements for parquet data format. 
Hive is used as a NoSql database as Hive allows queries against large datasets. 

Ideally the ETL stages would be separate jobs to allow better scalability but due to time constraints the extract, transform and load is done in a single spark job. 
Otherwise kafka and a scheduling tool like Oozie can be used to allow for a proper ETL pipeline. 

Also alot of values are hardcoded, including domain names, file locations, table names, and parquet files are hardcoded, if allowed more time configs can be externalised and configurable.
Jobs could be started in a separate tool like Jenkins to provide these types of parameters.

The docker-compose file does not configure and deploy bde2020-hive. This is deliberate as the db should be external to the project and sould be deployed separately.
It currently deploys a spark-master and worker setup to make resources available but does not start the app itself. This was done for testing purposes, but could be changed to start the application too. For testing purposes this will stay as is.

The project was originally set up to use maven but was changed to use sbt, as templates were found to use sbt. The templates allowed for a quicker development time to containerise the application.

During development different networks were not able to communicate due to setup issues locally. So everything was linked to the docker-hive_default network as a tactical fix. This should change in the future for spark and hive to exist in separate networks, but still able to communicate.
