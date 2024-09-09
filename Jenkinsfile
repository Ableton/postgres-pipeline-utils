library(identifier: 'ableton-utils@0.23', changelog: false)
library(identifier: 'groovylint@0.15', changelog: false)


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
        try {
          sh './gradlew test --warning-mode fail'
        } finally {
          junit 'build/test-results/**/*.xml'
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
