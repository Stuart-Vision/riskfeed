// Jenkins declarative pipeline (equivalent of the GitHub Actions build job).
pipeline {
    agent { docker { image 'maven:3.9-eclipse-temurin-21' } }
    options { timestamps() }
    stages {
        stage('Build and test') {
            steps { sh 'mvn -B verify' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }
        stage('Archive') {
            steps { archiveArtifacts artifacts: 'target/riskfeed-*.jar', fingerprint: true }
        }
    }
}
