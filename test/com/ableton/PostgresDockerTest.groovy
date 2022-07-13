package com.ableton

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test


class PostgresDockerTest extends BasePipelineTest {
  Object script

  @Override
  @Before
  @SuppressWarnings('ThrowException')
  void setUp() {
    super.setUp()

    this.script = loadScript('test/resources/EmptyPipeline.groovy')
    assertNotNull(script)
    script.env = [BUILD_ID: '1', JOB_BASE_NAME: 'TestJob']

    helper.addShMock('id -u', '1000', 0)
    helper.addShMock('id -g', '1000', 0)
    helper.registerAllowedMethod('error', [String]) { message ->
      throw new Exception(message)
    }
  }

  @Test
  void withDb() {
    String dataDir = 'tmpDirMocked/1/postgres/data'
    helper.addShMock("mkdir ${dataDir}", '', 0)
    helper.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)
    PostgresDocker postgres = new PostgresDocker(script: script)
    boolean bodyExecuted = false

    int bodyResult = postgres.withDb('testdb') { port, id ->
      bodyExecuted = true
      assertEquals('5432', port)
      assertEquals('mock-container', id)
      return 123
    }

    assertTrue(bodyExecuted)
    assertEquals(123, bodyResult)
  }

  @Test(expected = Exception)
  void withDbContainerFail() {
    String dataDir = 'tmpDirMocked/1/postgres/data'
    helper.addShMock("mkdir ${dataDir}", '', 0)
    helper.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 1)
    PostgresDocker postgres = new PostgresDocker(script: script)

    postgres.withDb('testdb') {}
  }

  @Test(expected = AssertionError)
  void withDbNoScript() {
    PostgresDocker postgres = new PostgresDocker()

    postgres.withDb('testdb') {}
  }

  @Test(expected = AssertionError)
  void withDbNoDbName() {
    PostgresDocker postgres = new PostgresDocker(script: script)

    postgres.withDb('') {}
  }

  @Test
  void withDbCustomPort() {
    helper.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)
    PostgresDocker postgres = new PostgresDocker(script: script, port: 1234)

    postgres.withDb('testdb') { port, id ->
      assertEquals('1234', port)
      assertEquals('mock-container', id)
    }
  }

  @Test
  void withDbRandomPort() {
    // Expected output given a seed of 1
    String expectedPort = '15873'
    helper.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)
    PostgresDocker postgres = new PostgresDocker(
      script: script, port: null, randomSeed: 1
    )

    postgres.withDb('testdb') { port, id ->
      assertEquals(expectedPort, port)
      assertEquals('mock-container', id)
    }
  }

  @Test(expected = AssertionError)
  void withLinkedContainerNoImage() {
    new PostgresDocker(script: script).withLinkedContainer(null, 'testdb') {}
  }

  @Test(expected = AssertionError)
  void withLinkedContainerNoDbName() {
    new PostgresDocker(script: script).withLinkedContainer('mock-image', null) {}
  }

  @Test
  void getRandomDigitString() {
    assertEquals('0897531194', PostgresDocker.getRandomDigitString(10, 0))
  }

  @Test
  void getRandomDigitStringIsRandom() {
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
  void getRandomDigitStringInvalidLength() {
    PostgresDocker.getRandomDigitString(0, 0)
  }
}
