package cn.enilu.common.code;

import java.util.Map;

/**
 * 基础的数据结构加载器<br>
 * </p> Copyright by easecredit.com<br>
 * 作者: zhangtao <br>
 * 创建日期: 16-7-12<br>
 */
public abstract class Loader {

    public abstract Map<String, TableDescriptor> loadTables(String configPath,
                                                   String basePackageName, String baseUri,String servPackageName,String modPackageName) throws Exception;


    }
