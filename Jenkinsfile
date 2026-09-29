pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        // Picked up by Maven 3.9+ for every ./mvnw call
        MAVEN_ARGS = '--batch-mode --no-transfer-progress'
    }

    stages {
        stage('Build') {
            steps {
                sh './mvnw clean compile'
            }
        }

        stage('Unit Tests') {
            steps {
                sh './mvnw test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                // No clean here: JaCoCo appends to the same jacoco.exec, so the report covers unit + integration tests
                sh './mvnw verify -DskipUnitTests=true'
            }
            post {
                always {
                    junit 'target/failsafe-reports/*.xml'
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                        sourceDirectories: [[path: 'src/main/java']]
                    )
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh './mvnw sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}
