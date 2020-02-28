#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-js-core/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])


try {
    // provide the name of the update site project
    knimetools.defaultTychoBuild('org.knime.update.workbench')

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar([])
    }
} catch (ex) {
    currentBuild.result = 'FAILED'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set ts=4: */
