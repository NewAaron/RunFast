#!/bin/bash

# 构建Docker镜像
echo "开始构建Docker镜像..."
docker build -t run-fast-game:1.0 .

# 检查构建结果
if [ $? -eq 0 ]; then
    echo "Docker镜像构建成功！"
    
    # 显示镜像信息
    echo "镜像信息："
    docker images | grep run-fast-game
    
    # 询问是否要运行容器
    read -p "是否要启动容器？(y/n): " answer
    if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
        # 运行容器
        echo "启动容器..."
        docker run -d -p 8080:8080 --name run-fast-game-container run-fast-game:1.0
        
        if [ $? -eq 0 ]; then
            echo "容器已成功启动！可以通过访问 http://localhost:8080 来访问应用。"
            echo "容器信息："
            docker ps | grep run-fast-game-container
        else
            echo "容器启动失败，请检查错误信息。"
        fi
    else
        echo "跳过容器启动。您可以稍后使用以下命令启动容器："
        echo "docker run -d -p 8080:8080 --name run-fast-game-container run-fast-game:1.0"
    fi
else
    echo "Docker镜像构建失败，请检查错误信息。"
fi
