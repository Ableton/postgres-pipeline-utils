package com.ableton


/**
 * Class for executing a closure with a connection to a PostgreSQL database in a Docker
 * container.
 */
class PostgresDocker implements Serializable {
  @SuppressWarnings('FieldTypeRequired')
  def script = null

  /**
   * Port to expose on the host for communicating with the Postgres instance. If null,
   * then a random port number between 10000-19999 will be used. In either case, the
   * resulting port will be passed as an argument to the closure argument of
   * {@link PostgresDocker#withDb} so that the caller can connect to the database.
   */
  String port = '5432'
  /**
   * Username to use for POSTGRES_USER.
   */
  String postgresUser = 'jenkins'
  /**
   * UID to use when for the Docker user mapping. If null, then the current user will be
   * automatically determined using a shell call to <pre>id -u</pre>.
   */
  String uid = null
  /**
   * Postgres version to use. This value must correspond to a valid tag name for the
   * Postgres Docker container. See: https://hub.docker.com/r/library/postgres/tags
   */
  String version = 'latest'

  /**
   * Seed for random number generator. Dy default, a unique seed will be used based on the
   * current time and the object's hashCode. Normally you shouldn't need to touch this
   * field.
   */
  protected long randomSeed = System.currentTimeMillis() * this.hashCode()

  /**
   * Execute a closure with a connection to a PostgreSQL database running in a Docker
   * container. The database will be stopped and all temporary files deleted when the
   * closure finishes executing.
   *
   * @param dbName Database name.
   * @param body Closure to execute.
   * @return Result of executing closure {@code body}.
   */
  @SuppressWarnings('MethodReturnTypeRequired')
  def withDb(String dbName, Closure body) {
    assert script
    assert dbName

    @SuppressWarnings('VariableTypeRequired')
    def bodyResult = null

    // Get the current directory so that the closure body is executed in the right place
    String pwd = script.pwd()
    String random = getRandomDigitString(8, randomSeed)
    String tempDir = "${script.pwd(tmp: true)}/${script.env.BUILD_ID}/postgres-${random}"
    script.dir(tempDir) {
      // Here we create a Dockerfile based on the postgres version, but with a custom UID
      // mapping. Without this, the postgres container runs into all sorts of weird
      // problems during initialization. Also, while we're at it, we can define the
      // POSTGRES_DB environment variable which will instruct the container to create
      // a database for us with the given name.
      String port = this.port ?: '1' + getRandomDigitString(4, randomSeed)
      String uid = this.uid ?: script.sh(returnStdout: true, script: 'id -u').trim()
      script.writeFile(
        file: 'Dockerfile',
        text: """
          FROM postgres:${version}
          RUN useradd --uid ${uid} --user-group ${postgresUser}
          ENV POSTGRES_USER=${postgresUser}
          ENV POSTGRES_DB=${dbName}
          USER ${postgresUser}
          EXPOSE 5432
          ENTRYPOINT ["/docker-entrypoint.sh", "postgres"]
        """
      )

      String imageName = script.env.JOB_BASE_NAME.toLowerCase()
      @SuppressWarnings('VariableTypeRequired')
      def postgresImage = script.docker.build(
        "${imageName}:${script.env.BUILD_ID}-${random}",
        '-f Dockerfile .'
      )

      // Start the newly built container. The postgres data dir must be mapped to our
      // temporary data directory or else initdb runs into permission problems when trying
      // to chmod the data dir to our custom UID.
      script.dir('data') {
        String dockerArgs = "-p ${port}:5432 -v ${script.pwd()}:/var/lib/postgresql/data"
        postgresImage.withRun(dockerArgs) { c ->
          // Wait for the database to come up for up to 30 seconds. Note that this command
          // is run from inside a new instance of the postgres container and linked to the
          // database container. By doing this, the postgres client does not need to be
          // installed on the build node. Note that when using docker.image.inside(), the
          // closure body is run instead of the entrypoint script.
          postgresImage.inside("--link ${c.id}:db") {
            script.retry(30) {
              script.sleep 1
              // This environment variable exposed by Docker always uses the port number
              // which is exposed (ie, the postgres port).
              script.sh "pg_isready -h \$DB_PORT_5432_TCP_ADDR"
            }
          }

          // Now the database should be up and running, so we can execute the body from
          // outside the container.
          script.dir(pwd) {
            bodyResult = body.call(port)
          }
        }
      }
      script.deleteDir()
      return bodyResult
    }
  }

  /**
   * Generates a string of random digits
   * @param length Number of characters to produce
   * @param seed Random seed (defaults to system time in milliseconds)
   */
  protected static String getRandomDigitString(int length, long seed) {
    if (length <= 0) {
      throw new IllegalArgumentException('Invalid string length')
    }
    String pool = '0123456789'
    @SuppressWarnings('InsecureRandom')
    Random random = new Random(seed)
    List randomChars = (0..length - 1).collect {
      pool[random.nextInt(pool.size())]
    }
    return randomChars.join('')
  }
}
