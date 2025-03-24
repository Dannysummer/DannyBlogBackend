-- 检查images表是否存在
SELECT COUNT(*) INTO @table_exists 
FROM information_schema.tables 
WHERE table_schema = DATABASE() AND table_name = 'images';

-- 如果表不存在，则创建
SET @create_table_sql = IF(@table_exists = 0, 
'CREATE TABLE images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL,
    thumbnail_url VARCHAR(500),
    type VARCHAR(50),
    size BIGINT NOT NULL,
    path VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    user_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;',
'SELECT "images表已存在" AS message');

PREPARE stmt FROM @create_table_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建索引
SET @create_index_sql = IF(@table_exists = 0,
'CREATE INDEX idx_images_type ON images(type);
CREATE INDEX idx_images_user_id ON images(user_id);
CREATE INDEX idx_images_created_at ON images(created_at);',
'SELECT "跳过索引创建" AS message');

PREPARE stmt FROM @create_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 