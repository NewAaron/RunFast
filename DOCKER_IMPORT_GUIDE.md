# Docker 镜像导入指南

这个文件包含将 `run-fast-game-1.0.tar` Docker 镜像导入其他 Docker 环境的步骤。

## 先决条件

- 目标机器上安装了 Docker（Docker Engine 或 Docker Desktop）
- 已将 `run-fast-game-1.0.tar` 文件复制到目标机器

## 导入步骤

1. **导入镜像**

   ```bash
   docker load -i run-fast-game-1.0.tar
   ```

2. **验证镜像已成功导入**

   ```bash
   docker images | grep run-fast-game
   ```

   您应该看到类似以下输出：
   ```
   run-fast-game   1.0       9aad922d847c   xxx       215MB
   ```

3. **运行容器**

   ```bash
   docker run -d -p 8080:8080 --name run-fast-container run-fast-game:1.0
   ```

4. **检查容器是否正在运行**

   ```bash
   docker ps | grep run-fast-container
   ```

5. **访问应用**

   在浏览器中打开 `http://localhost:8080` 即可访问应用。

## 常用 Docker 命令

- **查看容器日志**

  ```bash
  docker logs run-fast-container
  ```

  追踪日志：
  ```bash
  docker logs -f run-fast-container
  ```

- **停止容器**

  ```bash
  docker stop run-fast-container
  ```

- **启动已停止的容器**

  ```bash
  docker start run-fast-container
  ```

- **移除容器**

  ```bash
  docker rm run-fast-container
  ```

  如果容器仍在运行：
  ```bash
  docker rm -f run-fast-container
  ```

- **移除镜像**

  ```bash
  docker rmi run-fast-game:1.0
  ```

## 故障排除

1. **端口冲突**

   如果 8080 端口已被占用，可以更改映射端口：
   ```bash
   docker run -d -p 8081:8080 --name run-fast-container run-fast-game:1.0
   ```
   然后通过 `http://localhost:8081` 访问应用。

2. **查看容器详细信息**

   ```bash
   docker inspect run-fast-container
   ```

3. **进入容器内部**

   ```bash
   docker exec -it run-fast-container /bin/sh
   ```
