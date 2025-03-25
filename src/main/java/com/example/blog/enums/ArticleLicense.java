package com.example.blog.enums;

/**
 * 文章许可证枚举
 */
public enum ArticleLicense {
    /**
     * 知识共享 署名-非商业性使用-相同方式共享 4.0
     */
    CC_BY_NC_SA_4_0("CC BY-NC-SA 4.0", "知识共享 署名-非商业性使用-相同方式共享 4.0"),
    
    /**
     * 知识共享 署名-非商业性使用 4.0
     */
    CC_BY_NC_4_0("CC BY-NC 4.0", "知识共享 署名-非商业性使用 4.0"),
    
    /**
     * 知识共享 署名 4.0
     */
    CC_BY_4_0("CC BY 4.0", "知识共享 署名 4.0"),
    
    /**
     * 知识共享 署名-相同方式共享 4.0
     */
    CC_BY_SA_4_0("CC BY-SA 4.0", "知识共享 署名-相同方式共享 4.0"),
    
    /**
     * 知识共享 署名-禁止演绎 4.0
     */
    CC_BY_ND_4_0("CC BY-ND 4.0", "知识共享 署名-禁止演绎 4.0"),
    
    /**
     * 知识共享 署名-非商业性使用-禁止演绎 4.0
     */
    CC_BY_NC_ND_4_0("CC BY-NC-ND 4.0", "知识共享 署名-非商业性使用-禁止演绎 4.0"),
    
    /**
     * 公共领域
     */
    CC0_1_0("CC0 1.0", "公共领域"),
    
    /**
     * MIT许可证
     */
    MIT("MIT", "MIT许可证"),
    
    /**
     * Apache许可证2.0
     */
    APACHE_2_0("Apache 2.0", "Apache许可证2.0"),
    
    /**
     * GNU通用公共许可证3.0
     */
    GPL_3_0("GPL 3.0", "GNU通用公共许可证3.0"),
    
    /**
     * 版权所有，保留所有权利
     */
    ALL_RIGHTS_RESERVED("All Rights Reserved", "版权所有，保留所有权利");
    
    private final String code;
    private final String description;
    
    ArticleLicense(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据许可证代码查找枚举值
     */
    public static ArticleLicense fromCode(String code) {
        if (code == null) {
            return CC_BY_NC_SA_4_0; // 默认许可证
        }
        
        for (ArticleLicense license : values()) {
            if (license.code.equals(code)) {
                return license;
            }
        }
        
        return CC_BY_NC_SA_4_0; // 默认许可证
    }
} 