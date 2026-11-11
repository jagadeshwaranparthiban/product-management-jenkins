
pipeline {
    agent any

    environment {
        APP_NAME = 'product-management'
        AWS_REGION = 'ap-south-1'
        AWS_ACCOUNT_ID = '769863964654'

        ECR_REPOSITORY = 'product-management-api'
        ECR_REGISTRY = '769863964654.dkr.ecr.ap-south-1.amazonaws.com'
        ECR_URI = '769863964654.dkr.ecr.ap-south-1.amazonaws.com/product-management-api'

        EC2_INSTANCE_ID = 'i-0757a128504f7177a'
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

                            echo "Successfully pushed: $REMOTE_IMAGE"
                        '''
                    }
                }
            }
        }

        stage('Deploy to EC2 via SSM') {
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

                            echo "======================================"
                            echo "Starting EC2 deployment"
                            echo "Image: $REMOTE_IMAGE"
                            echo "Instance: $EC2_INSTANCE_ID"
                            echo "======================================"

                            # Validate image tag before passing it to SSM.
                            case "$IMAGE_TAG" in
                                *[!A-Za-z0-9._-]*|'')
                                    echo "Invalid Docker image tag."
                                    exit 1
                                    ;;
                            esac

                            echo "Verifying Jenkins AWS identity..."
                            aws sts get-caller-identity

                            # Construct SSM parameters as JSON.
                            COMMANDS_JSON=$(printf \
                                '{"commands":["/opt/product-management/deploy.sh %s"]}' \
                                "$IMAGE_TAG")

                            echo "Sending deployment command to EC2..."

                            COMMAND_ID=$(aws ssm send-command \
                                --instance-ids "$EC2_INSTANCE_ID" \
                                --document-name AWS-RunShellScript \
                                --parameters "$COMMANDS_JSON" \
                                --comment "Deploy product-management image $IMAGE_TAG" \
                                --timeout-seconds 600 \
                                --region "$AWS_REGION" \
                                --query "Command.CommandId" \
                                --output text)

                            echo "SSM Command ID: $COMMAND_ID"

                            STATUS="Pending"

                            # Wait up to 5 minutes for deployment.
                            for i in $(seq 1 60); do

                                STATUS=$(aws ssm get-command-invocation \
                                    --command-id "$COMMAND_ID" \
                                    --instance-id "$EC2_INSTANCE_ID" \
                                    --region "$AWS_REGION" \
                                    --query "Status" \
                                    --output text 2>/dev/null || true)

                                echo "Deployment status: $STATUS"

                                if [ "$STATUS" = "Success" ]; then
                                    break
                                fi

                                case "$STATUS" in
                                    Failed|Cancelled|TimedOut|Cancelling)
                                        echo "EC2 deployment failed."

                                        aws ssm get-command-invocation \
                                            --command-id "$COMMAND_ID" \
                                            --instance-id "$EC2_INSTANCE_ID" \
                                            --region "$AWS_REGION" \
                                            --query "[Status,StandardOutputContent,StandardErrorContent]" \
                                            --output text || true

                                        exit 1
                                        ;;
                                esac

                                sleep 5
                            done

                            if [ "$STATUS" != "Success" ]; then
                                echo "Deployment did not complete within the expected time."

                                aws ssm get-command-invocation \
                                    --command-id "$COMMAND_ID" \
                                    --instance-id "$EC2_INSTANCE_ID" \
                                    --region "$AWS_REGION" \
                                    --query "[Status,StandardOutputContent,StandardErrorContent]" \
                                    --output text || true

                                exit 1
                            fi

                            echo "======================================"
                            echo "EC2 deployment completed successfully"
                            echo "======================================"

                            aws ssm get-command-invocation \
                                --command-id "$COMMAND_ID" \
                                --instance-id "$EC2_INSTANCE_ID" \
                                --region "$AWS_REGION" \
                                --query "[Status,StandardOutputContent,StandardErrorContent]" \
                                --output text
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
            echo "Deployment completed on EC2."
        }

        failure {
            echo "Pipeline failed. Check the stage logs."
        }

        always {
            echo "Jenkins pipeline execution completed."
        }
    }
}
