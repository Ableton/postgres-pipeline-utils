import com.ableton.PostgresDocker


@SuppressWarnings('MethodReturnTypeRequired')
def withDb(String dbName, String postgresVersion, Closure body) {
  PostgresDocker postgresDocker = new PostgresDocker(script: this)
  return postgresDocker.withDb(dbName, postgresVersion, body)
}
