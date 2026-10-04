# ElmEats · 外卖点餐平台（前后端分离）

一个集成**用户鉴权、商家浏览、购物车、下单、在线支付、限时秒杀、AI 客服**的外卖点餐系统，共 9 张业务表、40+ 接口。
在完整业务闭环之上，重点做了**高并发防超卖**、**缓存设计**、**SQL 性能优化**与**安全加固**。

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Spring Boot 2.6、MyBatis、MySQL 8、Redis、RabbitMQ、JWT、支付宝沙箱 SDK |
| 前端 | Vue 3（`<script setup>`）、Vue Router、Axios |

## 功能清单

- **用户**：注册 / 登录（BCrypt + JWT）、个人资料修改、头像修改、退出登录
- **商家**：首页分类入口、按分类浏览商家、商家详情与菜品列表
- **购物车**：加购（数量累加、幂等）、修改数量、删除
- **订单**：从购物车下单（事务）、订单列表（分页 + 全部/未支付/已支付筛选）、订单明细
- **收货地址**：增删改查
- **在线支付**：支付宝沙箱电脑网站支付、异步通知验签、支付结果页状态轮询
- **限时秒杀**：秒杀活动列表（实时库存）、抢购、异步下单、抢购结果查询
- **AI 客服**：全局悬浮入口 + 聊天页，支持多轮上下文（DeepSeek）

## 技术亮点（均为实测数据）

| 亮点 | 实现 | 实测结果 |
|---|---|---|
| 秒杀防超卖 | Redis + Lua 原子预扣 → RabbitMQ 异步下单 → 数据库 `where stock > 0` + 唯一索引三层兜底 | **30 个用户并发抢 3 件库存，恰好 3 人成功、库存精确归零；同一用户并发 10 次仅 1 次成功**，全部请求 165ms 内返回 |
| 缓存设计 | 通用读缓存组件：空值占位符防穿透、SETNX 分布式锁防击穿、TTL 随机偏移防雪崩 | **热点 key 在 20 并发下仅回源查询 1 次** |
| SQL 优化 | MyBatis 三层嵌套子查询改单条 join；定位并修复「一对多 join + limit 导致分页重复/漏单」 | **订单列表查询从 16 条 SQL 降至 2 条** |
| 安全加固 | BCrypt 替换明文密码、JWT 无状态鉴权、身份从 token 解析 | **修复购物车 / 订单 / 收货地址等 4 个模块的水平越权漏洞** |
| 支付可靠性 | RSA2 验签 + 订单金额校验 + 幂等 + 结果页轮询 | 伪造的无签名通知被拒绝，订单状态不受影响 |

## 目录结构

```
elm/            后端（Spring Boot）
elmclient/      前端（Vue 3）
elm.sql         数据库脚本（含结构与演示数据）
seckill-test.ps1 秒杀防超卖压测脚本
```

## 快速开始

### 1. 环境要求

JDK 8、Maven、MySQL 8、Redis、RabbitMQ、Node.js 16+

### 2. 初始化数据库

```bash
mysql -uroot -p < elm.sql
```

脚本会创建 `elm` 库和 9 张表，并写入演示数据（含秒杀活动）。

### 3. 配置

`elm/src/main/resources/application.yml` 里的敏感项全是占位符，默认通过 `spring.profiles.active=local` 加载本地私密配置。两种方式任选：

**方式一（推荐，不会污染仓库）**：新建 `elm/src/main/resources/application-local.yml`，只写你本地的私密项：

```yaml
spring:
  datasource:
    password: 你的数据库密码
deepseek:
  api-key: sk-你的key
alipay:
  app-id: 你的沙箱APPID
  private-key: 你的应用私钥
  alipay-public-key: 支付宝公钥
  notify-url: https://你的公网域名/api/alipay/notify
```

该文件已被 `.gitignore` 忽略，不会被提交。

**方式二**：直接修改 `application.yml` 里的占位符（注意别把自己填的值提交上去）。

> 支付宝沙箱参数（AppID / 应用私钥 / 支付宝公钥）在 [open.alipay.com](https://open.alipay.com) → 沙箱环境 获取；异步通知需要一个公网地址（可用 ngrok 等内网穿透工具）。
> 只想跑基础功能的话，DeepSeek 和支付宝配置可以先留空，不影响登录、下单、秒杀。

### 4. 启动后端

```bash
cd elm
mvn spring-boot:run
```

### 5. 启动前端

```bash
cd elmclient
npm install
npm run serve
```

浏览器打开 `http://localhost:8081`（**必须是 8081 端口**，后端 CORS 白名单只放行了该端口）。

### 6. 演示账号

- 自己注册一个账号即可（注册需要填写收货地址才能参与秒杀）
- 或使用压测脚本自动创建的账号：`sk01` / `123456`

## 秒杀防超卖压测

脚本会自动准备测试账号、重置库存、并发发起抢购并校验结果（需要 PowerShell 7）：

```powershell
# 30 个用户同时抢 3 件库存，期望恰好 3 人成功
.\seckill-test.ps1 -SeckillId 3 -Users 30 -Stock 3

# 同一个用户并发 10 次，期望只有 1 次成功
.\seckill-test.ps1 -Mode repeat -SeckillId 1 -Users 10 -Stock 10
```

输出示例：

```
=== 3. 30 个用户同时抢购（库存 3 件） ===
  全部请求返回耗时：109 ms
     27 次 -> 已抢光，下次早点来～
      3 次 -> 抢购成功，正在为您生成订单...
  ✅ 数据库库存：0（未超卖）
  ✅ 落库的秒杀成功记录数 = 3（与响应一致，没有丢单/重复）
🎉 测试通过：没有超卖，也没有重复购买
```

## 备注

- 数据库脚本包含压测用的 30 个测试账号（`sk01`~`sk30`，密码统一 `123456`），便于直接复现压测
- 秒杀队列使用 `elm_` 前缀命名，避免与同一 RabbitMQ 上的其他项目冲突
