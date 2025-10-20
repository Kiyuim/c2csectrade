# 修复总结 (Fixes Summary)

## 已修复的问题

### 1. Lombok 编译错误
**问题**: 使用 JDK 23 时 Lombok 1.18.36 不兼容，导致编译失败
**解决方案**: 
- 创建了 `build-and-run.sh` 脚本，使用 JDK 17 进行编译
- 使用方法: `./build-and-run.sh`

### 2. 管理员登录问题
**问题**: 普通用户登录时也提示"需要管理员权限"
**解决方案**: 
- 修改了 `AuthController.java` 中的登录逻辑
- 现在只有当用户勾选"管理员登录"复选框但实际不是管理员时才会拒绝登录
- 普通用户不勾选管理员复选框可以正常登录
- 管理员可以选择是否勾选复选框登录

### 3. 验证码不显示问题
**问题**: 首次进入登录/注册页面时验证码不显示，需要刷新
**解决方案**: 
- 修改了 `LoginView.vue` 和 `RegisterView.vue`
- 改进了验证码 ID 的获取方式，从响应头中正确提取 `Captcha-ID`
- 验证码现在会在页面加载时自动显示

## 如何运行项目

### 方法 1: 使用快捷脚本（推荐）
```bash
./build-and-run.sh
```

### 方法 2: 手动运行
```bash
# 设置 JDK 17
export JAVA_HOME=/Users/kiyu/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home

# 构建项目
mvn clean package -DskipTests

# 启动服务
docker-compose up --build
```

## 验证码功能说明

1. **验证码自动加载**: 打开登录或注册页面时，验证码会自动加载
2. **刷新验证码**: 点击验证码图片可以刷新
3. **验证码验证**: 输入验证码后提交，后端会验证是否正确
4. **验证码过期**: 验证码在 5 分钟后自动过期

## 登录说明

### 普通用户登录
1. 输入用户名和密码
2. 输入验证码
3. **不要**勾选"管理员登录"复选框
4. 点击登录

### 管理员登录
1. 输入管理员用户名和密码
2. 输入验证码
3. 可以选择勾选"管理员登录"复选框（勾选后会验证是否真的是管理员）
4. 点击登录

## 测试账号

在 `init.sql` 中查看预设的测试账号：
- 普通用户: `user1` / `password1`
- 管理员: `admin` / `admin123`

## 访问地址

- 前端: http://localhost:8081
- 后端 API: http://localhost:8080
- MinIO 控制台: http://localhost:9001

## 故障排除

### 如果遇到 Lombok 编译错误
确保使用 JDK 17 编译:
```bash
export JAVA_HOME=/Users/kiyu/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home
java -version  # 应该显示 17.x.x
```

### 如果验证码不显示
1. 检查 Redis 是否正常运行: `docker-compose ps`
2. 检查浏览器控制台是否有错误
3. 尝试手动刷新验证码（点击验证码图片）

### 如果登录失败
1. 检查验证码是否输入正确（区分大小写）
2. 检查用户名密码是否正确
3. 如果是普通用户，确保没有勾选"管理员登录"
4. 查看浏览器控制台和后端日志获取详细错误信息

