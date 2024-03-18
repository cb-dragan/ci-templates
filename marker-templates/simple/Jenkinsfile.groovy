library identifier: 'ci-shared-library@main', retriever: modernSCM(
        [$class: 'GitSCMSource',
         remote: 'https://github.com/cb-ci-templates/ci-shared-library.git'])
library identifier: 'ci-shared-library_pt@main', retriever: modernSCM(
        [$class: 'GitSCMSource',
         remote: 'https://github.com/cb-ci-templates/ci-shared-library-pt.git'])

// Building the data object
def configYaml = """---
app : 'sample-app-helloworld'
k8_agent_yaml : 'podTemplate-curl.yaml'
branchPropertiesFile: "ci-config.properties"
param_greetings : 'Greetings to the rest of the World!'
default_key1: 'default_value1'
scanCheckmarx: true
"""
//Create a pipelineParams Map for the Pipeline template below
Map pipelineParams = readYaml text: "${configYaml}"
println pipelineParams

//###### START PIPELINE TEMPLATE###########
//We call the template from ci_shared_library_pt
pt_ci_simple (pipelineParams)