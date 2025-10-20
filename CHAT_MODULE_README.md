# 聊天模块实现说明

## 已完成的功能

### 后端实现 (Spring Boot + WebSocket)

#### 1. 数据库表
- **chat_message** 表：存储聊天消息
  - sender_id: 发送者ID
  - recipient_id: 接收者ID
  - content: 消息内容
  - timestamp: 发送时间
  - is_read: 是否已读

#### 2. 核心文件

**配置类:**
- `WebSocketConfig.java` - WebSocket和STOMP配置
- `WebSocketAuthInterceptor.java` - JWT认证拦截器

**实体和DTO:**
- `ChatMessage.java` - 聊天消息实体
- `ChatMessageDTO.java` - 数据传输对象

**数据层:**
- `ChatMessageMapper.java` - MyBatis映射器接口
- `ChatMessageMapper.xml` - SQL映射文件
- `ChatMessageRepository.java` - 数据访问层

**服务层:**
- `ChatMessageService.java` - 业务逻辑层

**控制器:**
- `ChatController.java` - WebSocket消息处理和REST API
  - WebSocket端点: `/app/chat.sendMessage`
  - REST API:
    - GET `/api/chat/history/{username}` - 获取聊天历史
    - POST `/api/chat/read/{username}` - 标记为已读
    - GET `/api/chat/unread-count` - 获取未读消息数
- `UserController.java` - 用户列表API
  - GET `/api/users` - 获取所有用户

### 前端实现 (Vue 3)

#### 1. 服务层
- `WebSocketService.js` - WebSocket连接管理
  - 自动携带JWT令牌认证
  - 订阅私有消息队列 `/user/queue/private`
  - 发送消息到 `/app/chat.sendMessage`
  
- `chatService.js` - REST API调用封装
  - 获取聊天历史
  - 标记消息为已读
  - 获取未读消息数

#### 2. 组件
- `ChatWindow.vue` - 聊天窗口组件
  - 消息列表展示
  - 实时接收新消息
  - 发送消息功能
  - 自动滚动到底部
  - 消息时间格式化
  
- `ChatView.vue` - 聊天主页面
  - 左侧联系人列表
  - 右侧聊天窗口
  - 未读消息数量显示
  - 用户头像和状态

#### 3. 路由
- `/chat` - 聊天页面路由（需要登录）

## 运行步骤

### 1. 安装前端依赖

```bash
cd frontend
npm install sockjs-client @stomp/stompjs
```

### 2. 更新数据库

数据库脚本 `init.sql` 已包含 `chat_message` 表的创建语句。
如果数据库已运行，需要重新运行初始化脚本或手动执行表创建语句。

### 3. 编译后端

```bash
# 在项目根目录
mvn clean package -DskipTests
```

### 4. 启动后端

```bash
# 方式1: 使用Maven
mvn spring-boot:run

# 方式2: 使用jar包
java -jar target/c2c-platform-0.0.1-SNAPSHOT.jar
```

### 5. 启动前端

```bash
cd frontend
npm run serve
```

## 测试步骤

### 1. 创建测试用户
- 注册至少两个用户账号（例如：user1, user2）

### 2. 登录第一个用户
- 使用 user1 登录
- 点击导航栏的"聊天"链接
- 在左侧联系人列表中选择 user2

### 3. 发送消息
- 在聊天输入框输入消息
- 点击"发送"按钮
- 消息会立即显示在聊天窗口

### 4. 登录第二个用户（另一个浏览器或无痕窗口）
- 使用 user2 登录
- 点击导航栏的"聊天"链接
- 在左侧联系人列表中选择 user1
- 应该能看到 user1 发送的历史消息

### 5. 双向实时聊天
- 在两个浏览器窗口中分别以 user1 和 user2 身份登录
- 在任一窗口发送消息
- 另一窗口应该实时收到消息（无需刷新）

## 技术特性

### WebSocket连接
- 使用 STOMP 协议over WebSocket
- SockJS 作为降级方案（支持不兼容WebSocket的浏览器）
- JWT认证集成

### 消息传输
- 点对点私信模式
- 使用 `/user/{username}/queue/private` 队列
- 服务器端使用 `convertAndSendToUser` 精确推送

### 数据持久化
- 所有消息存储到MySQL数据库
- 支持查询历史消息
- 支持消息已读/未读状态

### 前端特性
- Vue 3 Composition API
- 响应式实时更新
- 消息发送即时反馈
- 自动滚动到最新消息
- 美观的UI设计

## 架构优势

1. **安全性**: JWT认证保护WebSocket连接
2. **可扩展性**: 清晰的分层架构，易于扩展功能
3. **实时性**: WebSocket提供真正的实时通信
4. **可靠性**: 消息持久化，支持离线消息查看
5. **用户体验**: 流畅的界面，即时反馈

## 后续优化建议

1. **消息提醒**: 添加桌面通知或声音提醒
2. **在线状态**: 显示用户在线/离线状态
3. **表情支持**: 支持emoji表情
4. **图片发送**: 支持图片消息
5. **消息撤回**: 支持消息撤回功能
6. **群聊功能**: 支持多人群组聊天
7. **消息搜索**: 支持关键词搜索历史消息
8. **商品关联**: 根据商品页面快速联系卖家

