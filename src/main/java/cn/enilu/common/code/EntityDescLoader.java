package cn.enilu.common.code;

import com.google.common.collect.Maps;

import org.nutz.dao.entity.annotation.*;

import org.nutz.lang.Files;
import org.nutz.lang.Mirror;
import org.nutz.lang.Strings;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URLDecoder;

import java.util.Map;

/**
 * 根据java model定义生成，service，controller， <br>
 * </p> Copyright by easecredit.com<br>
 * 作者: zhangtao <br>
 * 创建日期: 16-7-10<br>
 */
public class EntityDescLoader extends  Loader {
    @Override
    public Map<String, TableDescriptor> loadTables(String configPath, String basePackageName, String baseUri, String servPackageName, String modPackageName) throws Exception {
        String packageName = basePackageName+"."+modPackageName;

        String filePath = packageName.replaceAll("\\.","\\/");
        String   path = Loader.class.getClassLoader().getResource(filePath).getPath();
        String abstractPath = URLDecoder.decode(path, "utf8");
        File[] files = Files.lsFile(abstractPath, null);
        Map<String, TableDescriptor> tables = Maps.newHashMap();

        for(File file:files){
            String fileName = file.getName().split("\\.")[0];
            String className = packageName+"."+fileName;
            Class modelClass = Class.forName(className);
            if(className.contains(".Model")){
                continue;
            }

            Mirror mirror = Mirror.me(modelClass);
             Comment comment = (Comment) mirror.getAnnotation(Comment.class);
            String commentStr = fileName;
            if(comment!=null) {
                  commentStr = comment.value();
            }
            Table table = (Table) mirror.getAnnotation(Table.class);
            String tableName = table.value();
            TableDescriptor tableDescriptor = new TableDescriptor(tableName,basePackageName,baseUri,servPackageName,modPackageName);
            tableDescriptor.setLabel(commentStr);
            tables.put(tableName, tableDescriptor);
            Field[] fields = mirror.getFields();
            mirror.getFields();
            for(Field field:fields){
                ColumnDescriptor column = new ColumnDescriptor();
                String fieldName = field.getName();
                if(fieldName.equals("opBy")||fieldName.equals("opAt")||fieldName.equals("delFlag")){
                    continue;
                }
                Annotation[] annotations = field.getAnnotations();
                for(Annotation annotation :annotations){
                    if(annotation instanceof  Comment){
                        column.setLabel(((Comment) annotation).value());
                    }
                    if(annotation instanceof  Id||annotation instanceof Name){
                        column.primary=true;
                    }
                    if(annotation instanceof ColDefine){
                        ColType colType = ((ColDefine) annotation).type();
                        column.setColumnType(colType.name());
                        column.dataType = colType.name();
                    }
                    if(annotation instanceof  Column){
                        String columnName = ((Column) annotation).value();
                        if(Strings.isBlank(columnName)){
                            column.columnName = fieldName;
                        }else {
                            column.columnName = columnName;
                        }
                    }


                }
                tableDescriptor.addColumn(column);
            }


        }
        return tables;
    }
}
