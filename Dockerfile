# 第一阶段：构建应用
FROM maven:3.8-openjdk-8 AS build

# 设置工作目录
WORKDIR /app

# 首先复制 pom.xml 文件
COPY pom.xml .
# 为了更好地利用Docker缓存，先下载依赖
RUN mvn dependency:go-offline -B

# 然后复制源代码
COPY src ./src

# 构建应用
RUN mvn package -DskipTests

# 第二阶段：运行应用
FROM openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app

# 从构建阶段复制编译好的jar文件
COPY --from=build /app/target/*.jar app.jar

# 暴露应用端口（假设应用在8080端口运行，如需更改请修改）
EXPOSE 8080

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]

# 健康检查（可选）
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/ || exit 1
