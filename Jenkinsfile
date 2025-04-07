pipeline {
    agent any
    environment {
        SONAR_SCANNER_HOME = '/opt/sonar-scanner'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Rushikesh-me/cicd-assignment.git'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Static Code Analysis') {
            steps {
                sh 'mvn sonar:sonar -Dsonar.projectKey=cicd-assignment-key -Dsonar.host.url=http://54.217.181.39:9000 -Dsonar.login=squ_b131e237b3323974eca44adb20bd124d0a804098'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Code Coverage') {
            steps {
                sh 'mvn jacoco:report'
            }
        }
        stage('Docker Build') {
            steps {
                sh 'docker build -t rushikesh0028/productservice:v1 .'
            }
        }
        stage('Debug Environment') {
            steps {
                sh 'DOCKER_HOST=unix:///var/run/docker.sock ansible-playbook -i inventory_file deploy.yml'
            }
        }
        stage('Deploy') {
            steps {
                sh '''
                  export DOCKER_HOST=unix:///var/run/docker.sock
                  echo "DOCKER_HOST is $DOCKER_HOST"
                  ansible-playbook deploy.yml
                '''
            }
        }
    }
    post {
        always {
            junit 'target/surefire-reports/*.xml'
            jacoco execPattern: 'target/jacoco.exec'
        }
    }
}
