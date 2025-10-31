#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2026-06'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-js-core/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])


try {
    parallel (
        'Tycho Build': {
            knimetools.defaultTychoBuild('org.knime.update.workbench')
        },
        'Integrated Workflowtests': {
            workflowTests.runIntegratedWorkflowTests(profile: 'test')
        },
    )

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
