Please use the following command to get the latest database from the database backup:
mongorestore -d db_gpms <directory_backup where the db_gpms is stored>


To drop database: 
mongo db_gpms --eval "db.dropDatabase()"