# postgres-pipeline-utils

`postgres-pipeline-utils` is a Jenkins shared pipeline library which makes it easier to
run commands against Postgres in a Docker container. For information on using this library
in your Jenkins pipeline scripts, please refer to the [Jenkins shared library
documentation][jenkins-shared-lib-usage].


## Usage

Example usage of this library might look something like this:

```groovy
stage('Test') {
  postgres.withDb('testdb', '9.6') {
    sh 'run-db-tests.sh'
  }
}
```

The above code snippet will pull the Postgres Docker image for version 9.6, and start the
container. The commands in the closure body will be executed *outside* of the container on
the node.

The library also makes sure that the container is fully started and ready to communicate
with before executing the closure body. To do this, it calls `pg_isready` until it
succeeds. Although it catches and suppresses errors from the failed calls while the
container is starting, you may see some red X's in Blue Ocean's output. They can be safely
ignored.


[jenkins-shared-lib-usage]: https://jenkins.io/doc/book/pipeline/shared-libraries/#using-libraries
