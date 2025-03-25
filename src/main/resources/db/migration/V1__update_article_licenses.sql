-- 修复文章license字段，将字符串值转换为枚举名称
UPDATE articles SET license = 'CC_BY_NC_SA_4_0' WHERE license = 'CC BY-NC-SA 4.0';
UPDATE articles SET license = 'CC_BY_NC_4_0' WHERE license = 'CC BY-NC 4.0';
UPDATE articles SET license = 'CC_BY_4_0' WHERE license = 'CC BY 4.0';
UPDATE articles SET license = 'CC_BY_SA_4_0' WHERE license = 'CC BY-SA 4.0';
UPDATE articles SET license = 'CC_BY_ND_4_0' WHERE license = 'CC BY-ND 4.0';
UPDATE articles SET license = 'CC_BY_NC_ND_4_0' WHERE license = 'CC BY-NC-ND 4.0';
UPDATE articles SET license = 'CC0_1_0' WHERE license = 'CC0 1.0';
UPDATE articles SET license = 'MIT' WHERE license = 'MIT';
UPDATE articles SET license = 'APACHE_2_0' WHERE license = 'Apache 2.0';
UPDATE articles SET license = 'GPL_3_0' WHERE license = 'GPL 3.0';
UPDATE articles SET license = 'ALL_RIGHTS_RESERVED' WHERE license = 'All Rights Reserved';

-- 设置默认值
UPDATE articles SET license = 'CC_BY_NC_SA_4_0' WHERE license IS NULL OR license = ''; 