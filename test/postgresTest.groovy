import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before


@SuppressWarnings('ClassName')
class postgresTest extends BasePipelineTest {
  def postgres

  @Override
  @Before
  void setUp() {
    super.setUp()
    this.postgres = new postgres()
  }
}
