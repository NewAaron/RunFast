# 关牌（跑得快）

这是一个基于 Java 1.8 + Spring Boot + WebSocket 的关牌（跑得快）三人卡牌游戏，支持房间创建、加入、实时出牌。游戏规则类似于“赖子斗地主”，但简化为三人对战模式。

## 技术栈

- 后端：Java 1.8, Spring Boot 2.7.18, WebSocket
- 前端：HTML5, CSS3, JavaScript

## 功能特性

- 实时多人游戏，支持3个玩家同时在线对战
- 房间系统，创建和加入房间
- 玩家准备系统
- 实时出牌和PASS系统
- 牌型判定和大小比较
- 胜负判定
- 响应式界面，适配桌面和移动设备
- 醒目的视觉和声音提示，增强游戏体验

## 游戏规则

1. 三个玩家，每人17张牌（共51张，去掉♠2）
2. 持有♠3的玩家先手
3. 可用牌型：
   - 单张
   - 对子
   - 三张
   - 顺子（5张及以上连续单牌）
   - 连对（2组及以上连续对子）
   - 三带二（三张加一对）
   - 炸弹（四张相同点数）
   - 同花顺（同花色的顺子）
   - 四带一（四张相同点数加1张单牌）
4. 后出牌必须同类型且大于前一手牌（炸弹除外）
5. 炸弹可压任何牌型
6. 任何两家连续PASS后，下一家可重新出任意牌型
7. 首位出完手牌的玩家获胜

## 系统架构

### 后端结构

- `config/`: Spring配置
  - `WebSocketConfig.java`: WebSocket配置

- `controller/`: 控制器
  - `GameWebSocketHandler.java`: 处理WebSocket消息和游戏逻辑

- `model/`: 数据模型
  - `Card.java`: 卡牌模型
  - `Player.java`: 玩家模型
  - `Room.java`: 游戏房间
  - `Game.java`: 游戏逻辑

- `service/`: 服务
  - `RoomService.java`: 房间管理服务

### 前端结构

- `index.html`: 主要界面，包含大厅和游戏界面
  - 卡牌显示和选择逻辑
  - WebSocket通信
  - 响应式界面设计

## 运行方法

### 使用Maven

```bash
./mvnw spring-boot:run
```

或

```bash
mvn spring-boot:run
```

### 使用JAR文件

```bash
java -jar target/cardgame-0.0.1-SNAPSHOT.jar
```

### 使用Docker

```bash
# 构建Docker镜像
./build-docker.sh

# 运行Docker容器
docker-compose up
```

然后在浏览器中访问 http://localhost:8080

## 最近修复

### 问题修复
- 修复了前端出牌与后端识别顺序不一致问题
- 优化了按钮交互体验，确保出牌环节中按钮始终可用
- 增加了错误提示和声音反馈
- 优化了卡牌渲染和选择逻辑

## 部署

### Docker 部署
项目包含完整的 Docker 部署配置：

```bash
# 构建 Docker 镜像
./build-docker.sh

# 使用 docker-compose 启动服务
docker-compose up -d
```

### 开发环境
1. 克隆此仓库
```bash
git clone https://github.com/NewAaron/RunFast.git
```

2. 进入项目目录
```bash
cd RunFast
```

3. 使用 Maven 构建和运行
```bash
./mvnw clean package
./mvnw spring-boot:run
```

4. 浏览器访问 http://localhost:8080

## 贡献指南
欢迎提交 Issue 和 Pull Request 来改进这个项目！

## 许可证
MIT License

1. **前端出牌与后端识别不一致问题**
   - 前端渲染时对手牌进行了排序，但发送出牌请求时使用的是相对于未排序数组的索引
   - 修复了前端出牌时使用排序后的卡牌对象直接发送，而不是使用索引

2. **黑桃3识别问题**
   - 增强了前后端卡牌匹配逻辑，确保黑桃3等特殊牌型能被准确识别
   - 添加了更多日志输出以便调试

3. **UI交互优化**
   - 出牌和PASS按钮仅在轮到玩家时可点击
   - 轮到玩家出牌时有视觉和声音提示
   - 优化了不同分辨率下的界面自适应

### 详细技术改进

1. **前端修改**：
   - 在卡牌DOM元素中添加了原始索引和卡牌值的数据属性
   - 修改出牌逻辑，直接使用卡牌值而不是基于索引
   - 增强了卡牌显示和选择的可靠性
   - 更新了卡牌渲染逻辑，保存索引映射关系

2. **后端修改**：
   - 优化卡牌匹配算法，通过多种方式验证卡牌
   - 增强了日志记录，更容易排查问题
   - 修复了黑桃3检测逻辑
   - 改进了卡牌对象的比较逻辑

## 游戏截图

[游戏界面截图]

## 扩展功能（待实现）

- 玩家头像和个人资料
- 游戏历史记录
- 排行榜系统
- 聊天功能
- 更多牌型（如连三带一对）
- 游戏结算系统
- 观战模式
- 游戏回放

## 贡献

欢迎提交Pull Request或Issue。

## 许可证

[MIT License](LICENSE)
