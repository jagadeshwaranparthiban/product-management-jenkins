pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'product-management'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                echo 'Running Maven build and tests...'

                sh '''
                    chmod +x mvnw
                    ./mvnw -B clean verify
                '''
            }
        }

        stage('Publish Test Results') {
            steps {
                junit '**/target/surefire-reports/*.xml'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def imageTag = "${APP_NAME}:${BUILD_NUMBER}"

                    sh """
                        docker build -t ${imageTag} .
                    """

                    echo "Docker image created: ${imageTag}"
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts(
                artifacts: 'target/site/jacoco/**',
                allowEmptyArchive: true
            )
        }

        success {
            echo 'CI pipeline completed successfully!'
        }

        failure {
            echo 'CI pipeline failed. Check the stage logs.'
        }
    }
}
