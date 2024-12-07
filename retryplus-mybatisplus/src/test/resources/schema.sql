CREATE TABLE bean_retry_record
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    retry_times          INT          DEFAULT 0,
    max_retry_times      INT          DEFAULT 0,
    bean_class           VARCHAR(255) DEFAULT '',
    bean_method          VARCHAR(255) DEFAULT '',
    param_values         TEXT,
    method_param_types   TEXT,
    real_param_types     TEXT,
    exception_msg        TEXT,
    retry_result         INT          DEFAULT 0,
    next_retry_timestamp BIGINT,
    create_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
