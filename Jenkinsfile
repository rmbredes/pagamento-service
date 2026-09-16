// Pipeline do pagamento-service executada pelo Jenkins.
pipeline {

    // Utiliza o mesmo agente Windows com Java 21
    // já configurado para o ProjetoSpringBoot.
    agent {
        label 'windows && java21'
    }

    // Solicita ao Jenkins a instalação cadastrada do JDK 21.
    tools {
        jdk 'JDK21'
    }

    // Configurações reutilizadas pelos stages.
    environment {

        // Região onde criamos o repositório ECR.
        AWS_REGION = 'sa-east-1'

        // Perfil configurado localmente no Windows.
        AWS_PROFILE = 'projeto-s3'

        // Registro privado pertencente à nossa conta AWS.
        ECR_REGISTRY = '033649548808.dkr.ecr.sa-east-1.amazonaws.com'

        // Repositório específico do pagamento-service.
        ECR_REPOSITORY = 'recomeco/pagamento-service'
    }

    stages {

        // Obtém o código configurado no job do Jenkins.
        stage('Checkout') {

            steps {

                // Remove arquivos deixados por execuções anteriores.
                deleteDir()

                // Baixa o repositório e a branch configurados no job.
                checkout scm
            }
        }

        // Confirma as ferramentas disponíveis no agente.
        stage('Verificar ambiente') {

            steps {

                bat 'java -version'
                bat 'echo JAVA_HOME=%JAVA_HOME%'
                bat 'docker --version'
                bat 'aws --version'
            }
        }

        // Compila, testa e gera o JAR.
        stage('Testar e gerar pacote') {

            steps {

                // clean verify já executa os testes e produz o pacote.
                bat 'call mvnw.cmd clean verify'
            }
        }

        // Constrói a imagem local da execução atual.
        stage('Construir imagem Docker') {

            steps {

                // --pull procura uma versão atualizada da imagem-base.
                //
                // BUILD_NUMBER é o número gerado pelo Jenkins
                // para identificar esta execução.
                bat '''
                    docker build --pull ^
                        --tag pagamento-service:%BUILD_NUMBER% ^
                        .
                '''

                // Confirma que a imagem foi criada.
                bat 'docker image inspect pagamento-service:%BUILD_NUMBER%'
            }
        }

        // Confirma que o Jenkins consegue utilizar o perfil AWS.
        stage('Validar identidade AWS') {

            steps {

                bat '''
                    aws sts get-caller-identity ^
                        --profile %AWS_PROFILE%
                '''
            }
        }

        // Autentica o Docker no registro privado do ECR.
        stage('Autenticar no Amazon ECR') {

            steps {

                bat '''
                    aws ecr get-login-password ^
                        --region %AWS_REGION% ^
                        --profile %AWS_PROFILE% ^
                    | docker login ^
                        --username AWS ^
                        --password-stdin %ECR_REGISTRY%
                '''
            }
        }

        // Adiciona tags próprias do Jenkins e envia ao ECR.
        stage('Publicar imagem no Amazon ECR') {

            steps {

                bat '''
                    docker tag ^
                        pagamento-service:%BUILD_NUMBER% ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-latest

                    docker tag ^
                        pagamento-service:%BUILD_NUMBER% ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-%BUILD_NUMBER%

                    docker push ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-latest

                    docker push ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-%BUILD_NUMBER%
                '''
            }
        }
    }
}