// Pipeline do pagamento-service executada pelo Jenkins.
pipeline {

    // Utiliza o agente Windows com Java 21.
    agent {
        label 'windows && java21'
    }

    // Evita o checkout automático do Jenkins.
    // Faremos um único checkout, de forma explícita.
    options {
        skipDefaultCheckout(true)
    }

    // Solicita ao Jenkins a instalação cadastrada do JDK 21.
    tools {
        jdk 'JDK21'
    }

    // Configurações reutilizadas pelos stages.
    environment {

        // Caminho completo da AWS CLI instalada para o usuário Ricardo.
        AWS_CLI = 'C:\\Users\\Ricardo\\AppData\\Local\\Programs\\Amazon\\AWSCLIV2\\aws.exe'

        // Faz a AWS CLI procurar o cache do "aws login"
        // e os arquivos .aws no perfil do usuário Ricardo.
        HOME = 'C:\\Users\\Ricardo'
        USERPROFILE = 'C:\\Users\\Ricardo'
        AWS_CONFIG_FILE = 'C:\\Users\\Ricardo\\.aws\\config'

        // Perfil que assume a role usada no laboratório.
        AWS_PROFILE = 'projeto-s3'

        // Região e endereço do ECR.
        AWS_REGION = 'sa-east-1'
        ECR_REGISTRY = '033649548808.dkr.ecr.sa-east-1.amazonaws.com'
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

        // Confirma as ferramentas e os arquivos necessários.
        stage('Verificar ambiente') {

            steps {

                bat 'java -version'
                bat 'echo JAVA_HOME=%JAVA_HOME%'
                bat 'docker --version'

                // Interrompe claramente caso o executável não exista.
                bat '''
                    if not exist "%AWS_CLI%" (
                        echo AWS CLI nao encontrada em: %AWS_CLI%
                        exit /b 1
                    )
                '''

                // Interrompe caso o arquivo de configuração não exista.
                bat '''
                    if not exist "%AWS_CONFIG_FILE%" (
                        echo Configuracao AWS nao encontrada em: %AWS_CONFIG_FILE%
                        exit /b 1
                    )
                '''

                // Usa o caminho completo, pois a AWS CLI não está
                // no PATH da conta LocalSystem.
                bat '"%AWS_CLI%" --version'
            }
        }

        // Compila, testa e gera o JAR.
        stage('Testar e gerar pacote') {

            steps {

                bat 'call mvnw.cmd clean verify'
            }
        }

        // Constrói a imagem local da execução atual.
        stage('Construir imagem Docker') {

            steps {

                // --pull procura uma versão atualizada da imagem-base.
                //
                // BUILD_NUMBER identifica esta execução do Jenkins.
                bat '''
                    docker build --pull ^
                        --tag pagamento-service:%BUILD_NUMBER% ^
                        .
                '''

                bat 'docker image inspect pagamento-service:%BUILD_NUMBER%'
            }
        }

        // Confirma que o Jenkins consegue utilizar o perfil AWS.
        stage('Validar identidade AWS') {

            steps {

                bat '''
                    "%AWS_CLI%" sts get-caller-identity --profile %AWS_PROFILE%
                '''
            }
        }

        // Autentica o Docker no registro privado do ECR.
        stage('Autenticar no Amazon ECR') {

            steps {

                // A senha temporária segue diretamente da AWS CLI
                // para o Docker; ela não é gravada em arquivo.
                bat '''
                    "%AWS_CLI%" ecr get-login-password --region %AWS_REGION% --profile %AWS_PROFILE% | docker login --username AWS --password-stdin %ECR_REGISTRY%
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