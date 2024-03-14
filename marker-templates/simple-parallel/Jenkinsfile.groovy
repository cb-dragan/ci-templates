library identifier: 'ci-shared-library@main', retriever: modernSCM(
        [$class: 'GitSCMSource',
         remote: 'https://github.com/cb-ci-templates/ci-shared-library.git'])


// Building the data object
def configYaml = """---
app : 'App Hello World'
k8_agent_yaml : 'podTemplate-curl.yaml'
param_greetings : 'Greetings to the rest of the World!'
default_key1: 'default_value1'
scanCheckmarx: false
"""
//Create a pipelineParams Map for the Pipeline template
Map pipelineParams = readYaml text: "${configYaml}"
println pipelineParams

/*
dynamicStages test list to generate dynamicStages
Need to be configured by parameters or properties
 */
//def dynamicStages = ["UnitTests", "IntegrationTests", "SmokeTests","RegressionTests","AccessibilityTests"]


//We could call the Pipeline template from a shared library method
//However, the more templates we add to the library the bigger the size of the shared library
//pipelineHelloWorld (pipelineParams)


//So we can use the template also directly from here instead calling a shared library function
pipeline {
    agent {
        kubernetes {
            yaml libraryResource("podtemplates/${pipelineParams.k8_agent_yaml}")
        }
    }
    parameters {
        string(name: 'greeting', defaultValue: "${pipelineParams.param_greetings}",
                description: 'How should I greet the world?')
    }
    stages {
        stage("Init") {
            steps {

                //Init from yaml. It uses the `readYaml` step which can not use defaults
                //initFromYaml "./ci-config.yaml"

                //init from properties with defaults  (here "default_key1" f.e)
                initFromProperties('ci-config.properties', pipelineParams)

                echo "###### SAMPLE OUTPUT OF VARS#####"
                echo "Pipeline parameter: ${params.greeting}"
                echo "Pipeline Template parameter: ${pipelineParams.app}"
                sh "echo default_key1 ${env.default_key1}"
                sh "echo branch_key1 ${env.key1}"
                //sh "echo property EXAMPLE_KEY1 from yaml config:${env.EXAMPLE_KEY1}"
                sh "echo property DEPLOY from default config:${env.deploy}"
                echo "###### END SAMPLE OUTPUT OF VARS#####"

            }
        }
        stage('Build') {
            steps {
                echo "here we execute the build"
            }
        }
        stage('Test') {
            steps {
                sh "echo UnitTests"
            }
        }
        stage('Quality Gate') {
            //Skip the stage on other branches, execute just on "main"
            //TODO: This is just an example on how to skip stages, we need to decide if scans should be done on each branch or not
            when {
                branch 'main'
            }
            /*
            We know the quality gates so we can use static parallel structure
             */
            parallel {
                stage("NexusIQ") {
                    stages {
                        stage("scan") {
                            steps {
                                sh "echo scan NexusIQ"
                            }
                        }
                        stage("results") {
                            steps {
                                sh "echo results NexusIQ"
                            }
                        }
                    }
                }
                stage("Sonar") {
                    stages {
                        stage("scan") {
                            steps {
                                sh "echo scan Sonar"
                            }
                        }
                        stage("results") {
                            steps {
                                sh "echo results Sonar"
                            }
                        }
                    }
                }
                stage("Checkmarx") {
                    when {
                        expression { "${env.scanCheckmarx}" }
                    }
                    stages {
                        stage("scan") {
                            steps {
                                sh "echo scan Checkmarx"
                            }
                        }
                        stage("results") {
                            steps {
                                sh "echo results Checkmarx"
                            }
                        }
                    }
                }
            }
            post {
                /**
                 see all post options https://www.jenkins.io/doc/book/pipeline/syntax/#post
                 */
                always {
                    echo "do something on success"
                }
                success {
                    echo "do something on success"
                }
            }
        }
        stage('Deploy') {
            steps {
                echo """Here deploy the artifacts to integration test environment"""
                evaluate("${env.deploy} ()")
            }
        }
        stage('PostDeployTest') {
            /*TODO: dynamic parallel stages are possible, however, they lead to more complexity and should be avoided when possible
             TODO: Verify: Instead of using dynamic parallel stages Maven parallel test  might be a better choice
             https://www.baeldung.com/maven-junit-parallel-tests
             TBD: What exactly  are the test types?
             * Junit Tests?
             * UI Tests (Selenium f.e.)
             *Cross Platform/Browser tests? -> Matrix parallel stages might be an option
             * ???
             Depending on the test types the parallel structure and approach might be different

             steps {
               // Create a parallel block for dynamic stages, not sure yet if dynamic is  required
               parallelTestStages dynamicStages
           }

            */
            parallel {
                stage("SeleniumTests") {
                    stages {
                        stage("test") {
                            steps {
                                sh "echo UnitTests"
                            }
                        }
                    }
                    post {
                        /**
                         see all post options https://www.jenkins.io/doc/book/pipeline/syntax/#post
                         */
                        always {
                            echo "do something on success"
                        }
                        success {
                            echo "do something on success"
                        }
                    }
                }
                stage("APITest") {
                    stages {
                        stage("test") {
                            steps {
                                sh "echo IntegrationTests"
                            }
                        }
                    }
                    post {
                        /**
                         see all post options https://www.jenkins.io/doc/book/pipeline/syntax/#post
                         */
                        always {
                            echo "do something on success"
                        }
                        success {
                            echo "do something on success"
                        }
                    }
                }
                stage("CocumberTest") {
                    stages {
                        stage("test") {
                            steps {
                                sh "echo SmokeTests"
                            }
                        }
                    }
                    post {
                        /**
                         see all post options https://www.jenkins.io/doc/book/pipeline/syntax/#post
                         */
                        always {
                            echo "do something on success"
                        }
                        success {
                            echo "do something on success"
                        }
                    }
                }
                stage("ToscaTest") {
                    stages {
                        stage("test") {
                            steps {
                                sh "echo AccessibilityTests"
                            }
                        }
                    }
                    post {
                        /**
                         see all post options https://www.jenkins.io/doc/book/pipeline/syntax/#post
                         */
                        always {
                            echo "do something on success"
                        }
                        success {
                            echo "do something on success"
                        }
                    }
                }
            }
            /*TODO: dynamic parallel stages are possible, however, they lead to more complexity and should be avoided when possible
              TODO: Verify: Instead of using dynamic parallel stages Maven parallel test  might be a better choice
              https://www.baeldung.com/maven-junit-parallel-tests
              TBD: What exactly  are the test types?
              * Junit Tests?
              * UI Tests (Selenium f.e.)
              *Cross Platform/Browser tests? -> Matrix parallel stages might be an option
              * ???
              Depending on the test types the parallel structure and approach might be different

              steps {
                // Create a parallel block for dynamic stages, not sure yet if dynamic is  required
                parallelTestStages dynamicStages
            }

             */
        }
    }
    post {
        /**
         see all post options https://www.jenkins.io/doc/book/pipeline/syntax/#post
         */
        always {
            echo "do something on success"
        }
        success {
            echo "do something on success"
        }
    }
}

