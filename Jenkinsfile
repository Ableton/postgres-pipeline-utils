library(identifier: 'ableton-utils@0.28', changelog: false)
library(identifier: 'groovylint@0.13', changelog: false)


devToolsProject.run(
  defaultBranch: 'main',
  test: { data ->
    parallel(
      groovydoc: {
        data['docs'] = groovydoc.generate()
      },
      groovylint: {
        groovylint.check('./Jenkinsfile,./*.gradle,**/*.groovy')
      },
      junit: {
        junitUtils.run(testResults: 'build/test-results/**/*.xml') {
          sh './gradlew test --warning-mode fail'
        }
      },
    )
  },
  publish: { data ->
    jupiter.publishDocs("${data['docs']}/", 'Ableton/postgres-pipeline-utils')
  },
  deploy: { data ->
    String versionNumber = readFile('VERSION').trim()
    version.tag(versionNumber)
    version.forwardMinorBranch(versionNumber)
  },
)
