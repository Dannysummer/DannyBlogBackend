-- 检查users表是否存在avatar字段
SELECT COUNT(*) 
INTO @avatar_exists
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'users' 
AND COLUMN_NAME = 'avatar';

-- 如果不存在，则添加avatar字段
SET @sql = IF(@avatar_exists = 0,
    'ALTER TABLE users ADD COLUMN avatar VARCHAR(255) DEFAULT NULL',
    'SELECT "avatar字段已存在，无需添加" AS message');
    
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 