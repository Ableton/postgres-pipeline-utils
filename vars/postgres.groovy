import com.ableton.PostgresDocker


@SuppressWarnings('MethodReturnTypeRequired')
def withDb(String dbName, Closure body) {
  return new PostgresDocker(script: this).withDb(dbName, body)
}


@SuppressWarnings('MethodReturnTypeRequired')
def withDb(Map arguments = [:], Closure body) {
  arguments['script'] = this
  String dbName = arguments['dbName']
  arguments.remove('dbName')
  return new PostgresDocker(arguments).withDb(dbName, body)
}
