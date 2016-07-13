package cn.enilu.common.code;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import org.atteo.evo.inflector.English;
import org.nutz.lang.Strings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 实体（表）基本信息描述类<br>
 * 作者: zhangtao <br>
 * 创建日期: 16-7-10<br>
 */
public class TableDescriptor {
	private final String basePackageName;
	private final String baseUri;
	private final List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
	
	public final String name;
	private String pkType;
	private String comment;
	private String label="项";
	private String serPackageName;
	private String modPackageName;


	public TableDescriptor(String name, String basePackageName, String baseUri,String serPackageName,String modPackageName) {
		this.name = name;
		this.basePackageName = basePackageName;
		this.serPackageName = serPackageName;
		this.modPackageName = modPackageName;
		if (!baseUri.endsWith("/")) {
			baseUri = baseUri + "/";
		}
		this.baseUri = baseUri;
	}
	
	public String getBaseUri() {
		return baseUri;
	}

	public String getName() {
		return name;
	}

	public String getPlural() {
		return English.plural(name);
	}

	public String getPkType() {
		return pkType;
	}

	public void setPkType(String pkType) {
		this.pkType = pkType;
	}

	public String getBasePackageName() {
		return basePackageName;
	}

	public List<ColumnDescriptor> getColumns() {
		return columns;
	}

	public void addColumn(ColumnDescriptor column) {
		columns.add(column);
	}

	public String getClassName() {
		return getEntityClassName();
	}

	public String getUriPrefix() {
		return baseUri + getPlural();
	}

	public String getViewBasePath() {
		return baseUri.replaceFirst("/", "") + getPlural();
	}

	public String getModPackageName() {
		return modPackageName;
	}

	public void setModPackageName(String modPackageName) {
		this.modPackageName = modPackageName;
	}

	public String getSerPackageName() {
		return serPackageName;
	}

	public void setSerPackageName(String serPackageName) {
		this.serPackageName = serPackageName;
	}

	public String getEntityClassName() {
		return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
	}

	public String getEntityFullClassName() {
		return basePackageName + "."+getModPackageName()+"." + getEntityClassName();
	}

	public String getServiceFullClassName() {
		return basePackageName + "."+getSerPackageName()+"." + getServiceClassName();
	}

	public String getEntityInstanceName() {
		return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
	}

	public String getEntityInstancesName() {
		return getEntityInstanceName() + "s";
	}

	public String getServiceInstanceName() {
		return getEntityInstanceName() + "Service";
	}

	public String getServiceClassName() {
		return getEntityClassName() + "Service";
	}

	public String getControllerClassName() {
		return getEntityClassName() + "Controller";
	}

	public void addPrimaryKeyColumn(String columnName) {
		for (ColumnDescriptor column : columns) {
			if (column.columnName.equals(columnName)) {
				column.primary = true;
				break;
			}
		}
	}
	
	public ColumnDescriptor getPrimaryColumn() {
		for (ColumnDescriptor column : columns) {
			if (column.primary) {
				return column;
			}
		}
		return null;
	}
	
	public String getPrimaryType() {
		ColumnDescriptor columnDescriptor = getPrimaryColumn();
		if (columnDescriptor == null) {
			return null;
		}
		
		return columnDescriptor.getSimpleJavaTypeName();
	}

	public String getTableAnnotation() {
		return "@Table";
	}

	public List<String> getImports() {
		Set<String> klasses = new LinkedHashSet<String>();

		for (ColumnDescriptor column : columns) {
			String klass = column.getJavaType();
			if (klass.startsWith("java.lang") || klass.indexOf('.') == -1) {
				continue;
			}
			klasses.add(column.getJavaType());
		}

		List<String> imports = new ArrayList<String>();
		imports.addAll(klasses);
		imports.add(null);

		imports.add(Serializable.class.getName());
		imports.add(null);

		klasses.clear();
		for (ColumnDescriptor column : columns) {
			for (ColumnDescriptor.Validation v : column.getValidations()) {
				klasses.add(v.klass.getName());
			}
		}
		if (klasses.size() > 0) {
			imports.addAll(klasses);
			imports.add(null);
		}

		return imports;
	}

	public List<ColumnDescriptor> getEnumColumns() {
		List<ColumnDescriptor> result = new ArrayList<ColumnDescriptor>();
		for (ColumnDescriptor column : columns) {
			if (column.isEnum()) {
				result.add(column);
			}
		}
		return result;
	}

	public String getToStringBody() {
		return null;
//		StringBuilder code = new StringBuilder();
//		code.append("MoreObjects.toStringHelper(this)");
//		int count = 0;
//		for (ColumnDescriptor column : columns) {
//			code.append(".add(\"").append(column.getFieldName()).append("\", ")
//					.append(column.getFieldName()).append(")");
//
//			count++;
//			if (count % 2 == 0) {
//				code.append("\n\t\t\t");
//			}
//		}
//		code.append(".toString()");
//		return code.toString();
	}

	public String getQueryColumns(String op) {
		List<String> result =  new ArrayList<String>();
		for (ColumnDescriptor column : columns) {
			if (op.equals(column.getQueryOperator())) {
				result.add("\"" + column.columnName + "\"");
			}
		}

		if (result.isEmpty()) {
			return null;
		}
		return Joiner.on(", ").join(result);
	}

	public List<ColumnDescriptor> getSearchableColumns() {
		List<ColumnDescriptor> result = new ArrayList<ColumnDescriptor>();

		for (ColumnDescriptor column : columns) {
			if (!Strings.isBlank(column.getQueryOperator())) {
				result.add(column);
			}
		}

		return result;
	}

	public List<ColumnDescriptor> getLabeledColumns() {
		List<ColumnDescriptor> result = new ArrayList<ColumnDescriptor>();

		for (ColumnDescriptor column : columns) {
			if (column.hasLabel() && !column.primary) {
				result.add(column);
			}
		}

		return result;
	}
	
	public String[] getLabeledColumnNames() {
		List<ColumnDescriptor> columnDescriptors = getLabeledColumns();
		List<String> result = new ArrayList<String>();
		for(ColumnDescriptor descriptor:columnDescriptors){
			result.add(descriptor.getFieldName());
		}
		return result.toArray(new String[0]);
//		return Lists.map(columnDescriptors, "fieldName").toArray(new String[0]);

	}
	
	public String getLabeledColumnNamesString() {
		StringBuilder buf = new StringBuilder();
		for (String name : getLabeledColumnNames()) {
			buf.append("\"").append(name).append("\"").append(", ");
		}
		if (buf.length() > 2) {
			buf.setLength(buf.length() - 2);
		}
		return buf.toString();
	}

	public List<ColumnDescriptor> getIndexColumns() {
		List<ColumnDescriptor> result = new ArrayList<ColumnDescriptor>();

		for (ColumnDescriptor column : columns) {
			if (!Strings.isBlank(column.getLabel())
					|| "created_at".equals(column.columnName)) {
				result.add(column);
			}
		}

		return result;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
		if (comment == null) {
			return;
		}

		Pattern labelPattern = Pattern.compile("label:\\s*([^,;，]+)");
		Matcher m = labelPattern.matcher(comment);
		if (m.find()) {
			this.label = m.group(1);
		}
	}

}