package com.ableton

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals
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

  @Test(expected = AssertionError)
  void withDbNoScript() throws Exception {
    PostgresDocker postgres = new PostgresDocker()
    postgres.withDb('testdb') {}
  }

  @Test(expected = AssertionError)
  void withDbNoDbName() throws Exception {
    PostgresDocker postgres = new PostgresDocker(script: script)
    postgres.withDb('') {}
  }

  @Test
  void withDbCustomPort() throws Exception {
    JenkinsMocks.addShMock('id -u', '1000', 0)
    JenkinsMocks.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)

    PostgresDocker postgres = new PostgresDocker(script: script, port: 1234)
    postgres.withDb('testdb') { port ->
      assertEquals('1234', port)
    }
  }

  @Test
  void withDbRandomPort() throws Exception {
    // Expected output given a seed of 1
    String expectedPort = '15873'
    JenkinsMocks.addShMock('id -u', '1000', 0)
    JenkinsMocks.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)

    PostgresDocker postgres = new PostgresDocker(
      script: script,
      port: null,
      randomSeed: 1,
    )
    postgres.withDb('testdb') { port ->
      assertEquals(expectedPort, port)
    }
  }

  @Test
  void withDbCustomUid() throws Exception {
    // Add a mock for `id -u` that would fail if invoked
    JenkinsMocks.addShMock('id -u', null, 1)
    JenkinsMocks.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)

    PostgresDocker postgres = new PostgresDocker(script: script, uid: 123)
    postgres.withDb('testdb') {}
  }

  @Test
  void getRandomDigitString() throws Exception {
    assertEquals('0897531194', PostgresDocker.getRandomDigitString(10, 0))
  }

  @Test
  void getRandomDigitStringIsRandom() throws Exception {
    // Ensure that allocating two objects in rapid succession will yield two different
    // random number generators. Naively seeding the random number generator with
    // System.currentTimeMillis() would not guarantee this to be the case.
    PostgresDocker pd1 = new PostgresDocker(script: script)
    PostgresDocker pd2 = new PostgresDocker(script: script)
    assertNotEquals(
      pd1.getRandomDigitString(4, pd1.randomSeed),
      pd2.getRandomDigitString(4, pd2.randomSeed)
    )
  }

  @Test(expected = IllegalArgumentException)
  void getRandomDigitStringInvalidLength() throws Exception {
    PostgresDocker.getRandomDigitString(0, 0)
  }
}
