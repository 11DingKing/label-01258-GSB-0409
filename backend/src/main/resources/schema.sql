-- AI 智能考试系统数据库初始化脚本 (SQLite)

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    role VARCHAR(20) DEFAULT 'STUDENT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 大模型配置表
CREATE TABLE IF NOT EXISTS llm_config (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    api_key VARCHAR(200) NOT NULL,
    base_url VARCHAR(200),
    model_name VARCHAR(50) NOT NULL,
    temperature DECIMAL(3,2) DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2000,
    is_default INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 试卷表
CREATE TABLE IF NOT EXISTS paper (
    id INTEGER PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    total_score INTEGER DEFAULT 100,
    duration_minutes INTEGER DEFAULT 120,
    creator_id INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES sys_user(id)
);

-- 题目表
CREATE TABLE IF NOT EXISTS question (
    id INTEGER PRIMARY KEY,
    paper_id INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    options TEXT,
    answer TEXT NOT NULL,
    score INTEGER DEFAULT 10,
    sort_order INTEGER DEFAULT 0,
    FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE
);

-- 考试记录表
CREATE TABLE IF NOT EXISTS exam_record (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    paper_id INTEGER NOT NULL,
    start_time DATETIME NOT NULL,
    submit_time DATETIME,
    total_score INTEGER,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    grading_status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (paper_id) REFERENCES paper(id)
);

-- 答题记录表
CREATE TABLE IF NOT EXISTS answer_record (
    id INTEGER PRIMARY KEY,
    exam_record_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    user_answer TEXT,
    score INTEGER,
    ai_comment TEXT,
    is_graded INTEGER DEFAULT 0,
    FOREIGN KEY (exam_record_id) REFERENCES exam_record(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id)
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id INTEGER PRIMARY KEY,
    user_id INTEGER,
    username VARCHAR(50),
    operation VARCHAR(50),
    method VARCHAR(200),
    params TEXT,
    ip VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 初始化管理员账号 (密码: admin123)
INSERT OR IGNORE INTO sys_user (id, username, password, nickname, role) 
VALUES (1, 'admin', '01f1041543333717a8422a62d750ce45', '管理员', 'ADMIN');

-- 初始化测试学生账号 (密码: 123456)
INSERT OR IGNORE INTO sys_user (id, username, password, nickname, role) 
VALUES (2, 'student', 'd29c3a792f92e4b506a63e5aa17c7cef', '测试学生', 'STUDENT');
