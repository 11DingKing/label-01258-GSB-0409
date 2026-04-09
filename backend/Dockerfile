# 多阶段构建 - 支持 ARM64 和 AMD64
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段 - 使用跨平台基础镜像
FROM eclipse-temurin:17-jre

WORKDIR /app

# 设置时区为上海
ENV TZ=Asia/Shanghai

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 创建数据目录
RUN mkdir -p /app/data

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:sqlite:/app/data/exam.db"]
