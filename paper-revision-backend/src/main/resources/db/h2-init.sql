-- H2 本地开发初始化脚本
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    nickname VARCHAR(50), email VARCHAR(100), phone VARCHAR(20),
    password VARCHAR(256), salt VARCHAR(64), avatar_url VARCHAR(512),
    is_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS llm_providers (
    id VARCHAR(64) PRIMARY KEY, name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50), base_url VARCHAR(256), api_key VARCHAR(512),
    default_model VARCHAR(100), enabled BOOLEAN DEFAULT TRUE,
    config_json TEXT, user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS papers (
    id VARCHAR(64) PRIMARY KEY, title VARCHAR(256), file_name VARCHAR(256),
    file_path VARCHAR(512), file_size BIGINT, file_type VARCHAR(50),
    parsed_text TEXT, page_count INTEGER, status VARCHAR(20) DEFAULT 'UPLOADED',
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(64) PRIMARY KEY, paper_id VARCHAR(64) NOT NULL,
    chunk_index INTEGER, content TEXT NOT NULL, embedding_id VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS agents (
    id VARCHAR(64) PRIMARY KEY, name VARCHAR(100) NOT NULL,
    description TEXT, system_prompt TEXT, model_provider VARCHAR(50),
    model_name VARCHAR(100), config_json TEXT, enabled BOOLEAN DEFAULT TRUE,
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS revision_results (
    id VARCHAR(64) PRIMARY KEY, paper_id VARCHAR(64) NOT NULL, requirement_id VARCHAR(64),
    original_text TEXT, revised_text TEXT, suggestion TEXT, references_used TEXT,
    confidence DOUBLE, status VARCHAR(20), user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS agent_execution_traces (
    id VARCHAR(64) PRIMARY KEY, session_id VARCHAR(64) NOT NULL, agent_id VARCHAR(64),
    user_id VARCHAR(64) NOT NULL, phase VARCHAR(50), step_type VARCHAR(50),
    input_data TEXT, output_data TEXT, model_calls INTEGER DEFAULT 0,
    tool_calls INTEGER DEFAULT 0, tokens_used INTEGER DEFAULT 0,
    duration_ms BIGINT, status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS evaluations (
    id VARCHAR(64) PRIMARY KEY, revision_result_id VARCHAR(64) NOT NULL,
    relevance_score DOUBLE, faithfulness_score DOUBLE,
    completeness_score DOUBLE, format_score DOUBLE,
    overall_score DOUBLE, feedback TEXT, evaluator_type VARCHAR(50),
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS agent_test_cases (
    id VARCHAR(64) PRIMARY KEY, name VARCHAR(200) NOT NULL,
    description TEXT, input_data TEXT NOT NULL, expected_output TEXT,
    ground_truth TEXT, metadata_json TEXT, source_dataset VARCHAR(100),
    difficulty VARCHAR(20), user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS agent_test_suites (
    id VARCHAR(64) PRIMARY KEY, name VARCHAR(200) NOT NULL,
    description TEXT, config_json TEXT, status VARCHAR(20) DEFAULT 'DRAFT',
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS agent_test_suite_cases (
    id VARCHAR(64) PRIMARY KEY, suite_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL, sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS agent_eval_reports (
    id VARCHAR(64) PRIMARY KEY, suite_id VARCHAR(64), agent_id VARCHAR(64),
    name VARCHAR(200), status VARCHAR(20), overall_score DOUBLE,
    trajectory_score DOUBLE, llm_judge_score DOUBLE,
    total_cases INTEGER, completed_cases INTEGER, passed_cases INTEGER,
    summary TEXT, config_json TEXT, user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS agent_eval_report_items (
    id VARCHAR(64) PRIMARY KEY, report_id VARCHAR(64) NOT NULL,
    case_id VARCHAR(64) NOT NULL, overall_score DOUBLE,
    trajectory_score DOUBLE, llm_score DOUBLE, passed BOOLEAN,
    feedback TEXT, trace_id VARCHAR(64), duration_ms BIGINT,
    tokens_used INTEGER, details_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted_at TIMESTAMP
);

-- Skill 使用日志（Skill 自动进化 Step 2 —— 使用数据持久化）
CREATE TABLE IF NOT EXISTS skill_usage_log (
    id VARCHAR(64) PRIMARY KEY,
    skill_id VARCHAR(64) NOT NULL,
    success BOOLEAN,
    duration_ms BIGINT,
    context_size INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
