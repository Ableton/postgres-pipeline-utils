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
 * @see com.ableton.PostgresDocker#withLinkedContainer(def, String, List, Closure)
 */
@SuppressWarnings(['MethodParameterTypeRequired', 'MethodReturnTypeRequired'])
def withLinkedContainer(def image, String dbName, List dockerArgs = [], Closure body) {
  Map arguments = [
    script: this,
    image: image,
    dbName: dbName,
    dockerArgs: dockerArgs,
  ]
  return withLinkedContainer(arguments, body)
}


/**
 * Execute a closure with a running Docker image linked to a PostgreSQL container. Both
 * containers will be stopped and all temporary PostgreSQL files will be deleted when
 * the closure finishes executing.
 *
 * @param arguments Map of arguments. See the documentation of the fields in the class
 *                  {@link com.ableton.PostgresDocker} for valid arguments to this map.
 *                  Note that this method expects required arguments for both
 *                  {@code withDb} and {@code withLinkedContainer}.
 * @return Result of executing closure {@code body}.
 * @see com.ableton.PostgresDocker#withLinkedContainer(def, String, List, Closure)
 */
@SuppressWarnings('MethodReturnTypeRequired')
def withLinkedContainer(Map arguments = [:], Closure body) {
  arguments['script'] = this

  @SuppressWarnings('VariableTypeRequired')
  def image = arguments['image']
  String dbName = arguments['dbName']
  List dockerArgs = arguments['dockerArgs']

  arguments.remove('image')
  arguments.remove('dbName')
  arguments.remove('dockerArgs')

  return new PostgresDocker(arguments).withLinkedContainer(
    image,
    dbName,
    dockerArgs,
    body,
  )
}
