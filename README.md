# EP - Graph visualization and documents indexing
### Installation
* Install Play! Framework:
    * Download and install Play! Framework: [play]
* Install PostgreSQL :
    * Download and install PostgreSQL server: [pgsql]

### Creating Database and Schema
* Create database in pgSQL
* Use the SQL file locate at db/database-schema.sql to create the database schema.

### Compile
* Compile using Play! activator:
    * Enter into project directory, initialize activator and compile:
    ```sh 
       $ cd ep 
       $ activator 
       [ep] $ compile
    ```
    It will download all dependencies and compile the application.

### Run application
* Launch application using activator:
```sh
    $ cd ep
    $ activator
    [ep] $ run
```
Application will start, usaully at http://localhost:9000




**Free Software**

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)

   [play]: <https://www.playframework.com>
   [pgsql]: <https://www.postgresql.org>
  

