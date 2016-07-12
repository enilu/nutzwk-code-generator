# nutzwk-code-generator

## 特点
- 和nutz其他代码生成器比较，主要具备的特点是代码量少，只有三个java类，和若干模板文件，一个数据库连接配置文件。
- 这么比貌似不厚道，因为其他生成器不仅功能强大，而且还有ui界面。

## 功能介绍
- 自动生成nutzwk的代码代码，包括entity、service、controller和view
- 根据事先建立好的数据库表，生成对应的entity，service，controller和view
- 包括功能：添加，修改，删除，批量删除，分页查询功能。
- 模板引擎使用velocity

## 使用手册

     -c,--config <arg>      spring datasource config file(classpath),default:code.json
     -ctr,--package <arg>   controller base package name,default:${package}/controllers
     -f,--force             force generate file even if file exists,default:false
     -h,--help              show help message
     -i,--include <arg>     include table pattern,default:all of tables
     -mod,--package <arg>   model base package name,default:${package}/models
     -o,--output <arg>      output directory, default is src/main/java
     -p,--package <arg>     base package name,default:cn.wizzer.modules
     -sev,--package <arg>   service base package name,default:${package}/services
     -u,--base-uri <arg>    base uri prefix, default is /
     -x,--exclude <arg>     exclude table pattern

##举例

### 1,根据表生成相关代码

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
    
        -i dic_country -p cn.wizzer.modules.back.sys  -u /private/sys
        
- 会生成目录结构如下的代码：
 
 ![生成代码结构图](code-structure.png)
 
### 2,根据java实体生成相关代码
- 准备 java model类：

    
        @Comment("国家")
        @Table("dic_country")
        public class DicCountry  implements Serializable {
            private static final long serialVersionUID = 1L;
            @Name
            @Prev(els = {@EL("uuid()")})
            private String id;            
            @Column
            @Comment("编码")
            @ColDefine(type = ColType.VARCHAR)
            private String code;            
            @Column
            @Comment("名称")
            @ColDefine(type = ColType.VARCHAR)
            private String name;            
            setter...
            getter...   
        }
      
- 运行Generator类的时候加上如下参数：         
    
        -i dic_country -p cn.wizzer.modules.back.sys  -u /private/sys

        
- 会生成和上图一致的代码
 
 
## 后续功能

- 根据实体约束生成相关验证代码
- 去掉hibernate-valid 依赖
- 其他...


## 用法

下载项目后可以直接生成本地项目，然后在要使用的项目中添加依赖：

安装到本地仓库：

    mvn install

在自己的项目中添加依赖

        <dependency>
            <groupId>cn.enilu</groupId>
            <artifactId>nutzwk-code-generator</artifactId>
            <version>1.0</version>
        </dependency>