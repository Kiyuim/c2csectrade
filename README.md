# C2C SecTrade - 智能二手商品交易平台

C2C SecTrade 是一个功能完善的企业级C2C（用户对用户）二手商品交易平台，基于**分布式微服务架构**和**AI智能推荐系统**，为用户提供安全、便捷、智能的闲置物品交易环境。

平台支持**数码产品、服装鞋包、图书音像、家居生活、运动户外、美妆个护、母婴玩具、食品保健**等十多个品类，采用现代化的前后端分离架构，深度集成了**Redis缓存、RabbitMQ消息队列、Elasticsearch全文搜索**等分布式组件，提供**即时通讯、智能推荐、砍价议价、信用评价、实时消息推送**等创新功能。

## 🌟 核心亮点

- **🤖 智能推荐系统**：融合协同过滤、神经网络(NCF)、实时推荐等多种算法
- **📊 A/B测试框架**：数据驱动的算法优化，支持多变体实验
- **⚡ 分布式架构**：Redis缓存、RabbitMQ消息队列、Elasticsearch搜索（支持关键词高亮）
- **🎯 多目标优化**：同时优化点击率、转化率、收入、多样性和新颖性
- **💬 实时通讯**：WebSocket即时聊天，系统消息推送
- **🔨 砍价系统**：社交化电商，邀请好友助力砍价
- **⭐ 信用体系**：评价，智能信用积分计算

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
  - **订单中心**：
    - **我买到的**：追踪作为买家的所有订单，支持支付、确认收货、评价等操作
    - **我卖出的**：查看作为卖家的所有订单，实时掌握销售情况
    - **砍价活动**：管理所有参与的砍价活动和助力记录
- **信用与评价体系**：
  - **买家评价**：交易完成后，买家可以对商品和卖家进行评分和评论，支持图片上传（最多5张）和匿名评价。
  - **信用积分**：系统根据卖家的交易量、好评率等自动计算信用分和等级，构建平台信任体系。
- **个性化功能**：
  - **收藏夹**：方便用户收藏和追踪感兴趣的商品。
  - **举报系统**：用户可举报违规商品或用户，由管理员审核处理。
  - **相关推荐**：在商品详情页根据分类推荐相似商品。
- **管理员后台**：提供用户管理、举报管理、聊天监控等强大的后台功能。

## 📸 项目展示

以下是平台的部分功能截图，展示了用户界面和核心功能：

### 1. 首页概览
![首页概览](images/1.png)

### 2. 商品发布和管理
<img src="images/2.png" width="45%" /> <img src="images/3.png" width="45%" />
<img src="images/4.png" width="45%" /> <img src="images/5.png" width="45%" />

### 3. 商品详情
<img src="images/6.png" width="45%" /> <img src="images/7.png" width="45%" />

### 4. 订单管理和购买商品
<img src="images/9.png" width="45%" /> <img src="images/10.png" width="45%" />

### 5. 更多功能展示
<details>
<summary>点击查看更多截图</summary>

|  |  |
|:---:|:---:|
| ![11](images/11.png) | ![12](images/12.png) |
| ![13](images/13.png) | ![14](images/14.png) |
| ![15](images/15.png) | ![16](images/16.png) |
| ![17](images/17.png) | ![18](images/18.png) |
| ![19](images/19.png) | ![20](images/20.png) |

</details>

### 后端技术
- **核心框架**: Spring Boot 3.2.3
- **认证与安全**: Spring Security + JWT
- **数据库**: MySQL 8.0 + MyBatis
- **分布式缓存**: Redis 7 (会话管理、分布式锁、推荐缓存)
- **消息队列**: RabbitMQ 3.12 (事件驱动架构)
- **搜索引擎**: Elasticsearch 8.11 (全文搜索)
- **对象存储**: MinIO (图片/视频存储)
- **实时通信**: Spring WebSocket + STOMP
- **构建工具**: Maven

### 前端技术
- **核心框架**: Vue.js 3.x
- **路由管理**: Vue Router 4.x
- **状态管理**: Pinia
- **HTTP客户端**: Axios
- **UI设计**: 自定义组件 + 响应式CSS + SweetAlert2 (现代化通知)
- **实时通信**: SockJS + StompJS

### 推荐系统技术

本平台实现了一个**企业级多层次混合推荐系统**，旨在为用户提供精准、实时、多样化的商品推荐。系统深度融合了多种先进算法和架构，并通过A/B测试框架持续优化效果。

- **多层次推荐架构**:
  - **协同过滤 (Item-Based CF)**: 基于用户浏览历史，通过商品共现矩阵和余弦相似度计算商品间的相似性。
  - **基于内容的推荐 (Content-Based)**: 利用TF-IDF和余弦相似度，分析商品名称、描述、类别、价格等特征，计算内容层面的相似度。
  - **热度排序 (Popularity-Based)**: 结合浏览、收藏、加购、购买等多种用户行为，并引入时间衰减机制，动态计算商品热度。

- **高级推荐模型**:
  - **神经网络协同过滤 (NCF)**: 采用深度学习模型，通过Embedding层、GMF和MLP的结合，学习用户与商品之间复杂的非线性关系，实现在线增量学习。
  - **实时推荐系统**: 基于5分钟滑动时间窗口，实时捕捉用户会话内的兴趣变化和全站商品热度趋势，提供毫秒级响应的会话感知推荐。

- **系统级优化**:
  - **A/B测试框架**: 支持多变体实验和流量动态分配，通过统计显著性检验，数据驱动地选择最优推荐策略。
  - **多目标优化 (MTO)**: 在推荐排序阶段，同时优化**点击率 (CTR)、转化率 (CVR)、收入 (Revenue)、多样性 (Diversity)、新颖性 (Novelty)** 五大目标。
  - **多样性重排序 (MMR)**: 在最终推荐列表中使用Maximal Marginal Relevance (MMR)算法，确保推荐结果既相关又多样，避免信息茧房。

- **高性能架构**:
  - **三级缓存策略**: 设计了热点数据、推荐结果、模型参数三级Redis缓存，P99响应时间低于200ms。
  - **异步处理与分布式计算**: 用户行为追踪、推荐计算、指标收集均采用异步消息队列（RabbitMQ）处理，核心算法支持离线批处理、在线增量学习和流式计算。
  - **Redis深度集成**: 广泛应用分布式锁、布隆过滤器、限流控制等技术，确保系统在高并发下的稳定性和数据一致性。

### DevOps
- **容器化**: Docker + Docker Compose
- **Web服务器**: Nginx (用于反向代理前端静态资源)
- **监控**: Spring Actuator
- **日志**: SLF4J + Logback

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

#### 方式一：使用 Makefile（推荐）

项目提供了便捷的 Makefile 脚本，可以一键完成编译和部署：

```bash
# 克隆仓库
git clone https://github.com/kiyuim/c2csectrade.git
cd c2csectrade

# 一键启动（自动编译前后端并启动所有服务）
make up

# 或者分步执行
make build-backend    # 编译后端
make build-frontend   # 编译前端
make up               # 启动所有服务

# 查看日志
make logs

# 停止服务
make down

# 重启服务
make restart
```

#### 方式二：手动编译部署

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
    使用Docker Compose一键启动所有服务。
    ```bash
    docker compose up --build -d
    ```

4.  **访问应用**
    -   **前端应用**: [http://localhost](http://localhost) (默认端口80)
    -   **后端API**: [http://localhost/api](http://localhost/api)
    -   **MinIO控制台**: [http://localhost:9001](http://localhost:9001) 
        - AccessKey: `minioadmin`
        - SecretKey: `minioadmin`
    -   **RabbitMQ管理界面**: [http://localhost:15672](http://localhost:15672)
        - 用户名: `admin`
        - 密码: `admin123`

### 初始账号

系统默认创建了以下测试账号：

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 拥有所有管理权限 |
| 卖家(Lv1) | seller_lvl1 | admin123 | 信用分100, 等级1 |
| 卖家(Lv2) | seller_lvl2 | admin123 | 信用分300, 等级2 |
| 卖家(Lv3) | seller_lvl3 | admin123 | 信用分600, 等级3 |
| 卖家(Lv4) | seller_lvl4 | admin123 | 信用分1000, 等级4 |
| 卖家(Lv5) | seller_lvl5 | admin123 | 信用分2000, 等级5 |

## 📚 功能文档

本项目为每个核心功能模块都提供了详细的Markdown文档，你可以通过阅读它们来深入了解系统的设计与实现：

- `项目概述.md`
- `Docker 配置完整指南.md`
- `分布式系统架构文档.md`
- `部署与使用指南.md`
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
