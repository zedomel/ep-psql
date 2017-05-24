# EP -  Visualization of scientic documents using Multidimensional Projection

### Screenshots

![Screenshot 1][image1]
![Screenshot 1][image2]

### Installation
* Install Play! Framework:
    * Download and install Play! Framework: [play]
* Install PostgreSQL :
    * Download and install PostgreSQL server: [pgsql]

### Compile
* Compile using Play! activator:
    * Enter into project directory, initialize activator and compile:
    ```sh 
       $ cd ep-psql
       $ activator 
       [ep-psql] $ compile
    ```
    It will download all dependencies and compile the application.

### Run application in Dev. mode
* Launch application using activator:
```sh
    $ cd ep-psql
    $ activator
    [ep-psql] $ run
```
Application will start, usually at http://localhost:9000

## DEPLOYING (FOR PRODUCTION ONLY)

### Preparing for Deploy

* Generating an application secret:

```sh
[ep-psql] $ playGenerateSecret
[info] Generated new secret: QCYtAnfkaZiwrNwnxIlR6CTfG3gf90Latabg5241ABR5W1uDFNIkn
[success] Total time: 0 s, completed 28/03/2014 2:26:09 PM
```

* Using dist task:

```sh
[ep-psql] $ dist
```

This produces a ZIP file containing all JAR files needed to run your application in the `target/universal` folder of your application.

* Running application:

Unzip the file located at `target/universal` and then run the script in the `bin` directory (on windows use `.bat` script):

```sh
$ unzip ep-psql-1.0-SNAPSHOT.zip
$ ep-psql-1.0-SNAPSHOT/bin/ep-psql -Dplay.crypto.secret=abcdefghijk
```

Replacing the application secret for your secret generated before.

**For more information see:** [Play Documentation: Deploying your application](https://www.playframework.com/documentation/2.5.x/Deploying)

**Free Software**

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)

   [play]: <https://www.playframework.com>
   [pgsql]: <https://www.postgresql.org>
  

[image1]: https://github.com/zedomel/ep-psql/raw/master/screenshots/image1.png
[image2]: https://github.com/zedomel/ep-psql/raw/master/screenshots/image2.png
