import com.ableton.PostgresDocker


/**
 * @see #withDb(Map, Closure)
 */
@SuppressWarnings('MethodReturnTypeRequired')
def withDb(String dbName, Closure body) {
  return new PostgresDocker(script: this).withDb(dbName, body)
}


/**
 * Execute a closure with a connection to a PostgreSQL database running in a Docker
 * container. The database will be stopped and all temporary files deleted when the
 * closure finishes executing.
 *
 * @param arguments Map of arguments. See the documentation of the fields in the class
 *                  {@link com.ableton.PostgresDocker} for valid arguments to this map.
 *                  Note that {@code dbName} <strong>must be defined</strong>.
 * @param body Closure to execute.
 * @return Result of executing closure {@code body}.
 */
@SuppressWarnings('MethodReturnTypeRequired')
def withDb(Map arguments = [:], Closure body) {
  arguments['script'] = this
  String dbName = arguments['dbName']
  arguments.remove('dbName')
  return new PostgresDocker(arguments).withDb(dbName, body)
}
