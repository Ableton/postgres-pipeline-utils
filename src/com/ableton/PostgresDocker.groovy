package com.ableton


class PostgresDocker implements Serializable {
  @SuppressWarnings('FieldTypeRequired')
  def script = null

  /**
   * Port to expose on the host for communicating with the Postgres instance.
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

  @SuppressWarnings('MethodReturnTypeRequired')
  def withDb(String dbName, Closure body) {
    assert script

    @SuppressWarnings('VariableTypeRequired')
    def bodyResult = null
    String tempDir = script.pwd(tmp: true) + "/${script.env.BUILD_ID}/postgres"

    // Here we create a Dockerfile based on the postgres version, but with a user mapping
    // that corresponds to the UID on the local machine. Without this, the postgres
    // container runs into all sorts of weird problems during initialization.
    // Also, while we're at it, we can define the POSTGRES_DB environment variable which
    // will instruct the container to create a database for us with the given name.
    String dockerfile = "${tempDir}/Dockerfile"
    String uid = this.uid ?: script.sh(returnStdout: true, script: 'id -u').trim()
    script.writeFile(
      file: dockerfile,
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
    def postgresImage =
      script.docker.build("${imageName}:${script.env.BUILD_ID}", "-f ${dockerfile} .")

    // Start the newly built container. The postgres data dir must be mapped to our
    // temporary data directory or else initdb runs into permission problems when trying
    // to chmod the data dir to our custom UID.
    script.dir("${tempDir}/data") {
      String dockerArgs = "-p ${port}:5432 -v ${script.pwd()}:/var/lib/postgresql/data"
      postgresImage.withRun(dockerArgs) { c ->
        // Wait for the database to come up, for up to 30 seconds. Note that this command
        // is run from inside a new instance of the postgres container and linked to the
        // database container. By doing this, the postgres client does not need to be
        // installed on the build node. Note that when using docker.image.inside(), the
        // closure body is run instead of the entrypoint script.
        postgresImage.inside("--link ${c.id}:db") {
          script.retry(30) {
            script.sleep 1
            script.sh "pg_isready -h \$DB_PORT_5432_TCP_ADDR"
          }
        }

        // Now the database should be up and running, so we can execute the body from
        // outside the container.
        bodyResult = body.call()
      }

      script.deleteDir()
      return bodyResult
    }
  }
}
