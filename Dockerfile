# Utiliza uma imagem que já contém o Java 21 necessário
# para executar nossa aplicação.
#
# JRE significa Java Runtime Environment: possui o necessário
# para executar Java, mas não inclui todas as ferramentas de desenvolvimento.
FROM eclipse-temurin:21-jre-jammy


# Define /app como diretório de trabalho dentro da imagem.
#
# Os próximos comandos serão executados considerando essa pasta.
WORKDIR /app


# Copia o JAR gerado pelo Maven no Windows para dentro da imagem.
#
# Dentro da imagem, o arquivo receberá o nome mais simples app.jar.
COPY target/pagamento-service-0.0.1-SNAPSHOT.jar app.jar


# Documenta que a aplicação utiliza a porta 8081.
#
# EXPOSE não publica a porta automaticamente. A publicação será
# feita posteriormente com -p 8081:8081 ou pelo Compose.
EXPOSE 8081


# Define o comando executado quando um container for
# criado a partir desta imagem.
ENTRYPOINT ["java", "-jar", "app.jar"]