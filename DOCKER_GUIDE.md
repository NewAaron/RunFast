# Docker 部署指南

本指南将帮助您将 Run Fast 卡牌游戏应用程序部署到 Docker 环境中。

## 前提条件

- 安装了 Docker (20.10.x 或更高版本)
- 安装了 Docker Compose (可选，用于使用docker-compose.yml)

## 选项1：使用脚本构建和运行

1. 确保您的目录中包含以下文件：
   - Dockerfile
   - .dockerignore
   - build-docker.sh
   - 以及项目文件

2. 运行构建脚本：
   ```bash
   ./build-docker.sh
   ```
   
   脚本将执行以下操作：
   - 构建Docker镜像
   - 显示构建的镜像信息
   - 询问是否启动容器
   - 如果选择启动，将在端口8080上运行应用程序

## 选项2：使用 Docker Compose

1. 确保您的目录中包含以下文件：
   - Dockerfile
   - .dockerignore
   - docker-compose.yml
   - 以及项目文件

2. 运行以下命令：
   ```bash
   docker-compose up -d
   ```
   
   这将构建镜像并在后台启动容器。

## 选项3：手动执行 Docker 命令

如果您想手动控制整个过程，可以按照以下步骤操作：

1. 构建镜像：
   ```bash
   docker build -t run-fast-game:1.0 .
   ```

2. 运行容器：
   ```bash
   docker run -d -p 8080:8080 --name run-fast-game-container run-fast-game:1.0
   ```

## 应用程序访问

无论使用哪种方法，成功启动容器后，您都可以通过以下方式访问应用程序：
- 浏览器访问：http://localhost:8080

## 实用 Docker 命令

- 查看正在运行的容器：`docker ps`
- 查看容器日志：`docker logs run-fast-game-container`
- 查看容器日志并持续更新：`docker logs -f run-fast-game-container`
- 停止容器：`docker stop run-fast-game-container`
- 删除容器：`docker rm run-fast-game-container`
- 删除镜像：`docker rmi run-fast-game:1.0`
- 进入容器内部：`docker exec -it run-fast-game-container sh`

## 问题排查

如果遇到问题，请检查：

1. Docker 服务是否正在运行
2. 端口 8080 是否被其他应用占用
3. 查看容器日志以了解详细错误信息

如果应用程序启动但无法访问，请检查防火墙设置和网络连接。
