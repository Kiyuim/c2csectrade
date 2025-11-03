# c2csectrade - 校园二手商品交易平台

c2csectrade 是一个功能完善的C2C（用户对用户）二手商品交易平台，旨在为校园用户提供一个安全、便捷、有趣的闲置物品交易环境。其功能已扩展至支持数码产品、服装鞋包、图书、家居生活等十多个品类。项目采用现代化的前后端分离架构，后端基于Spring Boot，前端基于Vue.js，并集成了即时通讯、在线议价、信用评价等多种创新功能。

## ✨ 核心功能

- **用户系统**：提供用户注册、登录、JWT认证、后台用户管理（封禁/解封）等功能。
- **商品系统**：支持发布、编辑、下架各类闲置商品，集成富文本编辑器和多图上传（MinIO对象存储）。
- **首页与浏览**：瀑布流展示在售商品，支持按分类、校区和关键词搜索。
- **社交化交易**：
  - **砍价功能**：用户可对心仪的商品发起砍价，邀请好友助力以获取更低价格。
  - **即时通讯**：基于WebSocket，买卖双方可以进行实时的文字聊天和售前咨询。
- **完整的交易流程**：
  - **购物车**：支持添加、移除、修改商品数量，并可合并结算。
  - **订单系统**：从购物车或商品页创建订单，支持多卖家订单自动拆分。
  - **模拟支付**：内置账户余额和支付密码系统，模拟真实的支付体验。
  - **订单历史**：用户可以追踪作为买家或卖家的所有订单状态。
- **信用与评价体系**：
  - **双向评价**：交易完成后，买卖双方可以互相评分和评论。
  - **信用积分**：系统根据交易量、好评率等自动计算用户信用分和等级，构建平台信任体系。
- **个性化功能**：
  - **收藏夹**：方便用户收藏和追踪感兴趣的商品。
  - **举报系统**：用户可举报违规商品或用户，由管理员审核处理。
  - **相关推荐**：在商品详情页根据分类推荐相似商品。
- **管理员后台**：提供用户管理、举报管理、聊天监控等强大的后台功能。

## 📸 项目截图

<!-- 在这里插入您的项目截图，例如： -->
<!-- ![截图描述1](path/to/screenshot1.png) -->
<!-- ![截图描述2](path/to/screenshot2.png) -->
<!-- ![截图描述3](path/to/screenshot3.png) -->



## 🚀 技术栈

- **后端**
  - **框架**: Spring Boot 3.x
  - **认证与安全**: Spring Security, JWT
  - **数据库**: MySQL 8.0, MyBatis
  - **实时通信**: Spring WebSocket + STOMP
  - **构建工具**: Maven

- **前端**
  - **框架**: Vue.js 3.x
  - **路由**: Vue Router 4.x
  - **状态管理**: Pinia
  - **HTTP客户端**: Axios
  - **UI组件**: 自定义组件与基础CSS
  - **实时通信**: SockJS, StompJS

- **存储**
  - **数据库**: MySQL
  - **对象存储**: MinIO

- **部署与运维**
  - **容器化**: Docker, Docker Compose
  - **Web服务器**: Nginx (用于反向代理前端静态资源)

## 🏛️ 系统架构

本项目采用经典的前后端分离架构：

1.  **用户端 (Browser)**: 用户通过浏览器访问Vue.js构建的单页面应用(SPA)。
2.  **Nginx**: 作为Web服务器，负责托管前端静态文件，并将所有`/api`开头的API请求反向代理到后端Spring Boot服务。
3.  **前端 (Vue.js)**: 负责渲染页面、处理用户交互和调用后端API。
4.  **后端 (Spring Boot)**: 负责处理业务逻辑、数据库操作、提供RESTful API、处理WebSocket连接等。
5.  **数据层**: 
    - **MySQL**: 存储核心业务数据，如用户信息、商品、订单等。
    - **MinIO**: 存储所有媒体文件，如商品图片、用户头像等。

## ⚡ 快速开始

推荐使用Docker Compose一键部署所有服务，这是最简单快捷的方式。

### 环境要求
- [Docker](https://www.docker.com/get-started/) 和 [Docker Compose](https://docs.docker.com/compose/install/)
- [Java 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) (用于本地编译)
- [Maven 3.8+](https://maven.apache.org/download.cgi) (用于本地编译)
- [Node.js 16+](https://nodejs.org/) (用于本地编译)

### 部署步骤

1.  **克隆仓库**
    ```bash
    git clone https://github.com/kiyuim/c2csectrade.git
    cd c2csectrade
    ```

2.  **编译项目**
    -   **编译后端 (Spring Boot)**
        ```bash
        mvn clean package -DskipTests
        ```
        这会在`target/`目录下生成`c2c-platform-0.0.1-SNAPSHOT.jar`。

    -   **编译前端 (Vue.js)**
        ```bash
        cd frontend
        npm install
        npm run build
        cd ..
        ```
        这会在`frontend/dist`目录下生成用于生产环境的静态文件。

3.  **启动服务**
    使用Docker Compose一键启动所有服务（包括Nginx, Spring Boot, MySQL, MinIO）。
    ```bash
    docker-compose up --build
    ```
    `--build`参数会确保Docker镜像基于最新的代码进行构建。

4.  **访问应用**
    -   **前端应用**: [http://localhost:8080](http://localhost:8080)
    -   **MinIO控制台**: [http://localhost:9001](http://localhost:9001) (AccessKey: `minioadmin`, SecretKey: `minioadmin`)

## 📚 功能文档

本项目为每个核心功能模块都提供了详细的Markdown文档，你可以通过阅读它们来深入了解系统的设计与实现：

- `用户认证与管理系统文档.md`
- `商品发布与管理系统文档.md`
- `购物车功能文档.md`
- `收藏夹功能文档.md`
- `订单与支付系统文档.md`
- `即时通讯系统文档.md`
- `举报功能文档.md`
- `推荐系统文档.md`
- `评价与信用积分系统文档.md`
- `砍价功能测试指南.md`
- `创新点与亮点技术.md`

## 协议

本项目采用 [MIT License](LICENSE) 开源协议。