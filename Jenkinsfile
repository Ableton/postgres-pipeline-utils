library(identifier: 'ableton-utils@0.22', changelog: false)
library(identifier: 'groovylint@0.12', changelog: false)


devToolsProject.run(
  test: { data ->
    parallel(failFast: false,
      groovydoc: {
        data['docs'] = groovydoc.generate()
      },
      groovylint: {
        groovylint.check('./Jenkinsfile,./*.gradle,**/*.groovy')
      },
      junit: {
        try {
          sh './gradlew test'
        } finally {
          junit 'build/test-results/**/*.xml'
        }
      },
    )
  },
  publish: { data ->
    jupiter.publishDocs("${data['docs']}/", 'AbletonDevTools/postgres-pipeline-utils')
  },
  deployWhen: { return devToolsProject.shouldDeploy() },
  deploy: { data ->
    String versionNumber = readFile('VERSION').trim()
    version.tag(versionNumber)
    version.forwardMinorBranch(versionNumber)
  },
)
