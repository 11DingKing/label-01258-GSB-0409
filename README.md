# AI 智能考试系统

## 1. How to Run

```bash
# 使用 Docker Compose 一键启动
docker-compose up --build -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 2. Services

| 服务 | 端口 | 说明 |
|------|------|------|
| backend | 8080 | 后端 API 服务 (Spring Boot) |
| frontend-admin | 8081 | 管理后台 (Vue 3) |

启动后访问：http://localhost:8081

## 3. 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 学生 | student | 123456 |

## 4. 题目内容

```
写一个考试系统，分为大模型管理模块，出卷模块，改卷模块，考试模块，成绩查询和排行榜模块。
前端用vue后端用springboot，数据库用sqlite，支持大模型自动化批卷，出题支持大模型辅助出卷。
```

---

## 项目介绍

AI 智能考试系统是一个基于大模型的智能考试平台，支持 AI 辅助出卷和自动批卷功能。

### 功能模块

- **大模型管理**：配置多个 LLM 服务商（OpenAI、DeepSeek、通义千问、智谱等）
- **试卷管理**：创建试卷、AI 辅助生成题目、发布试卷
- **在线考试**：学生参加考试、计时答题、自动保存
- **智能批卷**：客观题自动批改、主观题 AI 评分+评语
- **成绩查询**：查看成绩详情、答案对比、AI 评语
- **排行榜**：按试卷查看成绩排名

### 技术栈

- **后端**：Java 17 + Spring Boot 3 + MyBatis-Plus + SQLite
- **前端**：Vue 3 + Vite + Element Plus + Pinia + Axios
- **部署**：Docker + Docker Compose

### 项目结构

```
├── backend/                 # 后端服务 (Spring Boot)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── frontend-admin/          # 管理后台 (Vue 3)
│   ├── src/
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml       # Docker 编排文件
├── .gitignore
└── README.md
```

### 支持的大模型

| 服务商 | 默认 API 地址 | 推荐模型 |
|--------|---------------|----------|
| DMXAPI | https://www.dmxapi.cn/v1 | qwen-max, gpt-3.5-turbo |
| OpenAI | https://api.openai.com/v1 | gpt-4, gpt-3.5-turbo |
| DeepSeek | https://api.deepseek.com/v1 | deepseek-chat |
| 通义千问 | https://dashscope.aliyuncs.com/compatible-mode/v1 | qwen-turbo, qwen-max |
| 智谱 AI | https://open.bigmodel.cn/api/paas/v4 | glm-4 |

### API Key 配置说明

系统支持多种大模型服务商，您可以根据需要选择配置。如果没有配置有效的 API Key，系统将自动使用**模拟模式**进行出题和批卷。

**系统已预置一个有效的 API Key 供测试使用，可直接体验 AI 出题和批卷功能。**

#### API Key 获取方式

| 服务商 | 获取地址 | 说明 |
|--------|----------|------|
| DMXAPI | https://www.dmxapi.cn | 第三方代理服务，支持多种模型 |
| 通义千问 | https://dashscope.console.aliyun.com | 阿里云 DashScope 控制台 |
| DeepSeek | https://platform.deepseek.com | DeepSeek 开放平台 |
| OpenAI | https://platform.openai.com | OpenAI 官方平台 |
| 智谱 AI | https://open.bigmodel.cn | 智谱开放平台 |

#### 系统内配置步骤

1. 使用管理员账号登录系统
2. 进入「大模型管理」模块
3. 点击「添加配置」
4. 选择服务商，填写 API Key
5. 点击「测试连接」验证配置
6. 保存并设为默认

> **提示**：如果测试连接失败，请检查：
> - API Key 是否正确
> - API 地址是否正确（部分服务商需要自定义地址）
> - 网络是否能访问对应的 API 服务

---

## 5. 测试用大模型配置（DMXAPI）

为方便测试，系统提供了一个可用的 DMXAPI 配置，按以下步骤配置即可体验 AI 出题和批卷功能：

### 配置信息

| 配置项 | 值 |
|--------|-----|
| 服务商 | DMXAPI |
| API Key | `sk-j1R7Dz9J5lLR9eNLG8zQ53Nae19zk6LOMGwZZyAq8lupV5A7` |
| API 地址 | `https://www.dmxapi.cn/v1` （默认，可留空） |
| 推荐模型 | `qwen-max` 或 `gpt-3.5-turbo` |

### 配置步骤

1. **登录系统**
   - 访问 http://localhost:8081
   - 使用管理员账号登录：`admin` / `admin123`

2. **进入大模型管理**
   - 点击左侧菜单「大模型管理」

3. **添加配置**
   - 点击右上角「添加配置」按钮
   - 填写配置名称，如：`测试用DMXAPI`
   - 服务商选择：`DMXAPI`
   - API 密钥填写：`sk-j1R7Dz9J5lLR9eNLG8zQ53Nae19zk6LOMGwZZyAq8lupV5A7`
   - 模型名称选择：`qwen-max`（推荐）

4. **测试连接**
   - 点击「保存配置」
   - 在配置列表中点击「测试连接」
   - 显示「✅ 连接成功」表示配置正确

5. **设为默认**
   - 点击「设为默认」按钮
   - 该配置将用于 AI 出题和批卷

### 验证配置

配置完成后，可以通过以下方式验证：

1. **AI 出题测试**
   - 进入「试卷管理」→「创建试卷」
   - 填写试卷名称，选择学科
   - 点击「AI 生成题目」
   - 等待生成完成，查看生成的题目

2. **AI 批卷测试**
   - 发布一份试卷，使用学生账号完成考试
   - 管理员进入「批卷管理」
   - 点击「AI 批卷」，查看批改结果和评语

---

### 使用说明

1. **配置大模型**：管理员登录后，进入「大模型管理」配置 LLM API
2. **创建试卷**：进入「试卷管理」，使用 AI 辅助生成题目或手动添加
3. **发布试卷**：确认无误后发布试卷
4. **学生考试**：学生登录后在「考试中心」参加考试
5. **智能批卷**：管理员在「批卷管理」使用 AI 批改主观题
6. **查看成绩**：学生在「我的成绩」查看成绩和 AI 评语
