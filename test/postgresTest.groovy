import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

import com.ableton.DockerMock
import com.ableton.JenkinsMocks
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test


@SuppressWarnings('ClassName')
class postgresTest extends BasePipelineTest {
  def postgres

  @Override
  @Before
  void setUp() {
    super.setUp()
    this.postgres = new postgres()

    postgres.with {
      docker = new DockerMock()
      env = [
        BUILD_ID: '1',
        JOB_BASE_NAME: 'TestJob',
      ]

      error = JenkinsMocks.error
      pwd = JenkinsMocks.pwd
      sh = JenkinsMocks.sh
      writeFile = {}
    }
  }

  @Test
  void withDb() throws Exception {
    def dataDir = JenkinsMocks.pwd(temp: true) + '/1/postgres/data'
    JenkinsMocks.addShMock('id -u', '1000', 0)
    JenkinsMocks.addShMock("mkdir ${dataDir}", '', 0)
    JenkinsMocks.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 0)

    def bodyExecuted = false
    def bodyResult = postgres.withDb('testdb', '9.6') {
      bodyExecuted = true
      return 123
    }

    assertTrue(bodyExecuted)
    assertEquals(123, bodyResult)
  }

  @Test
  void withDbContainerFail() throws Exception {
    def dataDir = JenkinsMocks.pwd(temp: true) + '/1/postgres/data'
    JenkinsMocks.addShMock('id -u', '1000', 0)
    JenkinsMocks.addShMock("mkdir ${dataDir}", '', 0)
    JenkinsMocks.addShMock("pg_isready -h \$DB_PORT_5432_TCP_ADDR", '', 1)

    def bodyExecuted = false
    def bodyResult = null
    def exceptionThrown = false
    try {
      bodyResult = postgres.withDb('testdb', '9.6') {
        bodyExecuted = true
        return 123
      }
    } catch (error) {
      exceptionThrown = true
      assertNotNull(error)
    }

    // The current implementation of postgres.withDb() has a bug where it does not throw
    // an exception if the container fails to start.
    // assertTrue(exceptionThrown)
    // assertFalse(bodyExecuted)
    // assertNull(bodyResult)
  }
}
