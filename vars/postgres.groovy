import com.ableton.PostgresDocker


@SuppressWarnings('MethodReturnTypeRequired')
def withDb(String dbName, String postgresVersion, Closure body) {
  PostgresDocker postgresDocker = new PostgresDocker(script: this, version: postgresVersion)
  return postgresDocker.withDb(dbName, body)
}
