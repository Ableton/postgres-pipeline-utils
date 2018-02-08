package com.ableton

import static org.junit.Assert.assertNotNull

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before


class PostgresDockerTest extends BasePipelineTest {
  @SuppressWarnings('FieldTypeRequired')
  def script

  @Override
  @Before
  void setUp() {
    super.setUp()

    this.script = loadScript('test/resources/EmptyPipeline.groovy')
    assertNotNull(script)
  }
}
