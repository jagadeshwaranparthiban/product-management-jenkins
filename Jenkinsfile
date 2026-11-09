pipeline {
    agent any

    environment {
        APP_NAME = 'product-management'
        AWS_REGION = 'ap-south-1'
        AWS_ACCOUNT_ID = '769863964654'

        ECR_REPOSITORY = 'product-management-api'
        ECR_REGISTRY = '769863964654.dkr.ecr.ap-south-1.amazonaws.com'
        ECR_URI = '769863964654.dkr.ecr.ap-south-1.amazonaws.com/product-management-api'
    }

    options {
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
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
                    def commitSha = sh(
                        script: 'git rev-parse --short=12 HEAD',
                        returnStdout: true
                    ).trim()

                    env.IMAGE_TAG = "${commitSha}-${env.BUILD_NUMBER}"
                    env.LOCAL_IMAGE = "${env.APP_NAME}:${env.IMAGE_TAG}"
                    env.REMOTE_IMAGE = "${env.ECR_URI}:${env.IMAGE_TAG}"
                }

                sh '''
                    echo "Building Docker image: ${LOCAL_IMAGE}"

                    docker build -t "${LOCAL_IMAGE}" .
                    docker tag "${LOCAL_IMAGE}" "${REMOTE_IMAGE}"
                '''
            }
        }

        stage('Verify AWS CLI') {
            steps {
                withEnv(["PATH+AWS=/usr/local/bin"]) {
                    sh '''
                        set -e
                        echo "AWS CLI location:"
                        command -v aws

                        echo "AWS CLI version:"
                        aws --version
                    '''
                }
            }
        }

        stage('Push Image to Amazon ECR') {
            steps {
                withEnv(["PATH+AWS=/usr/local/bin"]) {
                    withCredentials([
                        string(
                            credentialsId: 'aws-access-key-id',
                            variable: 'AWS_ACCESS_KEY_ID'
                        ),
                        string(
                            credentialsId: 'aws-secret-access-key',
                            variable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        sh '''
                            set -e

                            export AWS_DEFAULT_REGION="$AWS_REGION"

                            echo "Verifying AWS identity..."
                            aws sts get-caller-identity

                            echo "Logging into Amazon ECR..."
                            aws ecr get-login-password \
                                --region "$AWS_REGION" |
                                docker login \
                                    --username AWS \
                                    --password-stdin "$ECR_REGISTRY"

                            echo "Pushing Docker image..."
                            docker push "$REMOTE_IMAGE"

                            echo "Successfully pushed to ECR: $REMOTE_IMAGE"
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully!"
            echo "Published image: ${env.REMOTE_IMAGE}"
        }

        failure {
            echo "Pipeline failed. Check the stage logs."
        }

        always {
            echo "Jenkins pipeline execution completed."
        }
    }
}
