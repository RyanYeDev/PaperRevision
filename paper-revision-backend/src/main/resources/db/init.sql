-- PaperRevision 数据库初始化脚本

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    password VARCHAR(256),
    salt VARCHAR(64),
    avatar_url VARCHAR(512),
    is_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- LLM提供商表
CREATE TABLE IF NOT EXISTS llm_providers (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50),
    base_url VARCHAR(256),
    api_key VARCHAR(512),
    default_model VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    config_json TEXT,
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 论文表
CREATE TABLE IF NOT EXISTS papers (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(256),
    file_name VARCHAR(256),
    file_path VARCHAR(512),
    file_size BIGINT,
    file_type VARCHAR(50),
    parsed_text TEXT,
    page_count INTEGER,
    status VARCHAR(20) DEFAULT 'UPLOADED',
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 参考文献表
CREATE TABLE IF NOT EXISTS reference_papers (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(256),
    file_name VARCHAR(256),
    file_path VARCHAR(512),
    file_size BIGINT,
    parsed_text TEXT,
    paper_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 返修需求表
CREATE TABLE IF NOT EXISTS revision_requirements (
    id VARCHAR(64) PRIMARY KEY,
    paper_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    requirement_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Agent定义表
CREATE TABLE IF NOT EXISTS agents (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    system_prompt TEXT,
    model_provider VARCHAR(50),
    model_name VARCHAR(100),
    config_json TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 文档分块表
CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(64) PRIMARY KEY,
    paper_id VARCHAR(64) NOT NULL,
    chunk_index INTEGER,
    content TEXT NOT NULL,
    embedding_id VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Agent执行追踪表
CREATE TABLE IF NOT EXISTS agent_execution_traces (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64),
    user_id VARCHAR(64) NOT NULL,
    phase VARCHAR(50),
    step_type VARCHAR(50),
    input_data TEXT,
    output_data TEXT,
    model_calls INTEGER DEFAULT 0,
    tool_calls INTEGER DEFAULT 0,
    tokens_used INTEGER DEFAULT 0,
    duration_ms BIGINT,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 返修结果表
CREATE TABLE IF NOT EXISTS revision_results (
    id VARCHAR(64) PRIMARY KEY,
    paper_id VARCHAR(64) NOT NULL,
    requirement_id VARCHAR(64),
    original_text TEXT,
    revised_text TEXT,
    suggestion TEXT,
    references_used TEXT,
    confidence DOUBLE PRECISION,
    status VARCHAR(20),
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 评估记录表
CREATE TABLE IF NOT EXISTS evaluations (
    id VARCHAR(64) PRIMARY KEY,
    revision_result_id VARCHAR(64) NOT NULL,
    relevance_score DOUBLE PRECISION,
    faithfulness_score DOUBLE PRECISION,
    completeness_score DOUBLE PRECISION,
    format_score DOUBLE PRECISION,
    overall_score DOUBLE PRECISION,
    feedback TEXT,
    evaluator_type VARCHAR(50),
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_papers_user_id ON papers(user_id);
CREATE INDEX IF NOT EXISTS idx_papers_status ON papers(status);
CREATE INDEX IF NOT EXISTS idx_llm_providers_user_id ON llm_providers(user_id);
CREATE INDEX IF NOT EXISTS idx_revision_requirements_paper_id ON revision_requirements(paper_id);
CREATE INDEX IF NOT EXISTS idx_revision_results_paper_id ON revision_results(paper_id);
CREATE INDEX IF NOT EXISTS idx_agent_execution_traces_session_id ON agent_execution_traces(session_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_paper_id ON document_chunks(paper_id);

-- 测试用例表
CREATE TABLE IF NOT EXISTS agent_test_cases (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    input_data TEXT NOT NULL,
    expected_output TEXT,
    ground_truth TEXT,
    metadata_json TEXT,
    source_dataset VARCHAR(100),
    difficulty VARCHAR(20),
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 测试套件表
CREATE TABLE IF NOT EXISTS agent_test_suites (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    config_json TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 测试套件-用例关联表
CREATE TABLE IF NOT EXISTS agent_test_suite_cases (
    id VARCHAR(64) PRIMARY KEY,
    suite_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 评估报告表
CREATE TABLE IF NOT EXISTS agent_eval_reports (
    id VARCHAR(64) PRIMARY KEY,
    suite_id VARCHAR(64),
    agent_id VARCHAR(64),
    name VARCHAR(200),
    status VARCHAR(20),
    overall_score DOUBLE PRECISION,
    trajectory_score DOUBLE PRECISION,
    llm_judge_score DOUBLE PRECISION,
    total_cases INTEGER,
    completed_cases INTEGER,
    passed_cases INTEGER,
    summary TEXT,
    config_json TEXT,
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 评估报告明细表
CREATE TABLE IF NOT EXISTS agent_eval_report_items (
    id VARCHAR(64) PRIMARY KEY,
    report_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL,
    overall_score DOUBLE PRECISION,
    trajectory_score DOUBLE PRECISION,
    llm_score DOUBLE PRECISION,
    passed BOOLEAN,
    feedback TEXT,
    trace_id VARCHAR(64),
    duration_ms BIGINT,
    tokens_used INTEGER,
    details_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
