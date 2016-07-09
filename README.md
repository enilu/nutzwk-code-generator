# nutzwk-code-generator

## nutzwk 自动生成代码
- 自动生成nutzwk的代码代码，包括entity、service、controller和view
- 根据事先建立好的数据库表，生成对应的entity，service，controller和view
- 包括功能：添加，修改，删除，批量删除，分页查询功能。
- 模板引擎使用velocity

### 使用手册

        usage: Main [options] [all|entity|service|controller|view]
         -c,--config <arg>     spring datasource config file(classpath)
         -f,--force            force generate file even if file exists
         -h,--help             show help message
         -i,--include <arg>    include table pattern
         -o,--output <arg>     output directory, default is src/main/java
         -p,--package <arg>    base package name
         -u,--base-uri <arg>   base uri prefix, default is /
         -x,--exclude <arg>    exclude table pattern

###举例

- 比如使用下面语句建表：

        CREATE TABLE `dic_country` (
          `id` varchar(32) NOT NULL,
          `code` varchar(32) DEFAULT NULL COMMENT 'label:值',
          `name` varchar(64) DEFAULT NULL COMMENT 'label:显示值',
          `createAt` int(30) DEFAULT NULL,
          `updateAt` int(30) DEFAULT NULL,
          PRIMARY KEY (`id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='label:国家号';


- 运行Generator类的时候加上如下参数：         
    
        -p cn.wizzer.modules.inventory -i dic_country -u /inventory
  
- 会生成目录结构如下的代码：
 
 ![生成代码结构图](code-structure.png)
 
 
 ### 备注
 -- 建议使用的时候，可以直接