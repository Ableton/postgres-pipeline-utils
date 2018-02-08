package com.ableton

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.After
import org.junit.Before
import org.junit.Test


class PostgresDockerTest extends BasePipelineTest {
  @SuppressWarnings('FieldTypeRequired')
  def script

  @Override
  @Before
  void setUp() {
    super.setUp()

    this.script = loadScript('test/resources/EmptyPipeline.groovy')
    assertNotNull(script)
    script.with {
      docker = new DockerMock()
      env = [
        BUILD_ID: '1',
        JOB_BASE_NAME: 'TestJob',
      ]
    }

    helper.with {
      registerAllowedMethod('deleteDir', [], null)
      registerAllowedMethod('dir', [String, Closure], JenkinsMocks.dir)
      registerAllowedMethod('error', [String], JenkinsMocks.error)
      registerAllowedMethod('pwd', [Map], JenkinsMocks.pwd)
      registerAllowedMethod('pwd', [], JenkinsMocks.pwd)
      registerAllowedMethod('retry', [int, Closure], JenkinsMocks.retry)
      registerAllowedMethod('sh', [Map], JenkinsMocks.sh)
      registerAllowedMethod('sh', [String], JenkinsMocks.sh)
      registerAllowedMethod('writeFile', [Map], null)
    }
  }

  @After
  void tearDown() throws Exception {
    JenkinsMocks.clearStaticData()
  }

  @Test
  void withDb() throws Exception {
    String dataDir = JenkinsMocks.pwd(tmp: true) + '/1/postgres/data'
    JenkinsMocks.addShMock('id -u', '1000', 0)
    JenkinsMocks.addShMock("mkdir ${dataDir}", '', 0)
    JenkinsMocks.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)

    PostgresDocker postgres = new PostgresDocker(script: script)
    boolean bodyExecuted = false
    int bodyResult = postgres.withDb('testdb') {
      bodyExecuted = true
      return 123
    }

    assertTrue(bodyExecuted)
    assertEquals(123, bodyResult)
  }

  @Test(expected = Exception)
  void withDbContainerFail() throws Exception {
    String dataDir = JenkinsMocks.pwd(tmp: true) + '/1/postgres/data'
    JenkinsMocks.addShMock('id -u', '1000', 0)
    JenkinsMocks.addShMock("mkdir ${dataDir}", '', 0)
    JenkinsMocks.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 1)

    PostgresDocker postgres = new PostgresDocker(script: script)
    postgres.withDb('testdb') {}
  }
}
