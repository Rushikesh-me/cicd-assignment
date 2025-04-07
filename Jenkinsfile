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
                sh 'mvn sonar:sonar -Dsonar.projectKey=cicd-assignment-key -Dsonar.host.url=http://http://ec2-54-217-181-39.eu-west-1.compute.amazonaws.com:9000 -Dsonar.login=sqb_7160cbd3778feb6692bae217c0db39984fe17c3a'
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
                sh 'docker build -t yourapp:latest .'
            }
        }
        stage('Deploy') {
            steps {
                sh 'ansible-playbook deploy.yml'
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
