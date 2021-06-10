package com.ableton

import com.cloudbees.groovy.cps.NonCPS


/**
 * Class for executing a closure with a connection to a PostgreSQL database in a Docker
 * container.
 */
class PostgresDocker implements Serializable {
  Object script = null

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
   * @param body Closure to execute. This closure will be passed the following parameters:
   *             <ul>
   *               <li>{@code port}: The port which Postgres is running on.</li>
   *               <li>{@code id}: The ID of the Postgres Docker container.</li>
   *             </ul>
   * @return Result of executing closure {@code body}.
   */
  Object withDb(String dbName, Closure body) {
    assert script
    assert dbName

    Object bodyResult = null

    // Get the current directory so that the closure body is executed in the right place
    String pwd = script.pwd()
    String random = getRandomDigitString(8, randomSeed)
    String tempDir = "${script.pwd(tmp: true)}/${script.env.BUILD_ID}/postgres-${random}"
    script.dir(tempDir) {
      String port = this.port ?: '1' + getRandomDigitString(4, randomSeed)
      String uid = script.sh(
        label: 'Get user ID',
        returnStdout: true,
        script: 'id -u',
      ).trim()
      String gid = script.sh(
        label: 'Get group ID',
        returnStdout: true,
        script: 'id -g',
      ).trim()

      Object postgresImage = script.docker.image("postgres:${version}")

      String commonArgs = "-u ${uid}:${gid} -v /etc/passwd:/etc/passwd:ro"
      script.dir('data') {
        String dbContainerArgs = "-p ${port}:5432 -e POSTGRES_HOST_AUTH_METHOD=trust" +
          " -e POSTGRES_USER=${postgresUser} -e POSTGRES_DB=${dbName}" +
          " -v ${script.pwd()}:/var/lib/postgresql/data"

        postgresImage.withRun("${commonArgs} ${dbContainerArgs}") { c ->
          // Wait for the database to come up for up to 30 seconds. Note that this command
          // is run from inside a new instance of the postgres container and linked to the
          // database container. By doing this, the postgres client does not need to be
          // installed on the build node. Note that when using docker.image.inside(), the
          // closure body is run instead of the entrypoint script.
          postgresImage.inside("${commonArgs} --link ${c.id}:db --entrypoint=") {
            script.retry(30) {
              script.sleep 1
              // This environment variable exposed by Docker always uses the port number
              // which is exposed (ie, the postgres port).
              script.sh(
                label: 'Check if postgres is ready',
                script: "pg_isready -h \$DB_PORT_5432_TCP_ADDR",
              )
            }
          }

          // Now the database should be up and running, so we can execute the body from
          // outside the container.
          script.dir(pwd) {
            bodyResult = body.call(port, c.id)
          }
        }
      }
      script.deleteDir()
      return bodyResult
    }
  }

  /**
   * Execute a closure with a running Docker image linked to a PostgreSQL container. Both
   * containers will be stopped and all temporary PostgreSQL files will be deleted when
   * the closure finishes executing.
   *
   * @param image Docker image. This must be created by the {@code docker} singleton.
   * @param dbName Database name.
   * @param dockerArgs List of arguments to pass to the user-specified Docker image.
   * @param body Closure to execute. This closure will be passed the following parameters:
   *             <ul>
   *               <li>
   *                 {@code port}: The port which Postgres is running on. This value
   *                 should be ignored. It is passed from the {@code withDb} function, but
   *                 due to the way that linked containers work, the exposed port is not
   *                 needed and the port will always be the default value of 5432.
   *               </li>
   *               <li>{@code id}: The ID of the Postgres Docker container.</li>
   *             </ul>
   * @return Result of executing closure {@code body}.
   */
  Object withLinkedContainer(
    Object image, String dbName, List dockerArgs = [], Closure body
  ) {
    assert image

    withDb(dbName) { port, id ->
      image.withRun("--link ${id}:postgres ${dockerArgs.join(' ')}") {
        return body()
      }
    }
  }

  /**
   * Generates a string of random digits
   * @param length Number of characters to produce
   * @param seed Random seed (defaults to system time in milliseconds)
   */
  @NonCPS
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
