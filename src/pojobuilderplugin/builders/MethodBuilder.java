package pojobuilderplugin.builders;

import static pojobuilderplugin.util.BuilderPlugInConstants.BRACKET_CLOSE;
import static pojobuilderplugin.util.BuilderPlugInConstants.BRACKET_OPEN;
import static pojobuilderplugin.util.BuilderPlugInConstants.BUILDER;
import static pojobuilderplugin.util.BuilderPlugInConstants.DOT;
import static pojobuilderplugin.util.BuilderPlugInConstants.FunctionToRemovePrefix;
import static pojobuilderplugin.util.BuilderPlugInConstants.NEW_LINE;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_CLOSE;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_CLOSE_CHAR;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_OPEN;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_OPEN_CHAR;
import static pojobuilderplugin.util.BuilderPlugInConstants.SEMI_COLON;
import static pojobuilderplugin.util.BuilderPlugInConstants.SPACE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import pojobuilderplugin.util.BuilderPlugInUtil;
import pojobuilderplugin.wrapper.MyField;

public class MethodBuilder extends BaseBuilder {

	public MethodBuilder() {
		super();
	}

	public MethodBuilder returns(String returnType) {
		strSourceBuilder.append(returnType);
		addSpace();
		return this;
	}

	public MethodBuilder name(String methodName) {
		BuilderPlugInUtil.errorIfNullOrBlank(methodName, "Method name");
		strSourceBuilder.append(methodName);
		strSourceBuilder.append(BRACKET_OPEN).append(BRACKET_CLOSE);
		return this;
	}

	public MethodBuilder name(String methodName, MyField myField) {
		BuilderPlugInUtil.errorIfNullOrBlank(methodName, "Method name");
		strSourceBuilder.append(methodName);
		strSourceBuilder.append(BRACKET_OPEN);
		if (myField != null) {
			strSourceBuilder.append(myField.getType() + SPACE + myField.getCanonicalName());
		}
		strSourceBuilder.append(BRACKET_CLOSE).append(PARENTHESIS_OPEN_CHAR).append(NEW_LINE);
		if (myField != null) {
			strSourceBuilder.append(myField.getActualName() + "=" + myField.getCanonicalName());
			strSourceBuilder.append(SEMI_COLON);
		}
		strSourceBuilder.append(NEW_LINE);
		strSourceBuilder.append("return this;").append(NEW_LINE).append(PARENTHESIS_CLOSE_CHAR).append(NEW_LINE);
		return this;
	}

	public MethodBuilder code(String strSourceCode) {
		strSourceBuilder.append(PARENTHESIS_OPEN);
		if (!BuilderPlugInUtil.isNullOrBlank(strSourceCode))
			strSourceBuilder.append(NEW_LINE).append(strSourceCode).append(NEW_LINE);
		strSourceBuilder.append(PARENTHESIS_CLOSE);
		return this;
	}

	private String getParamName(String className) {
		char[] name = className.toCharArray();
		if ((int) name[0] < 96) {
			// if first character is caps(since the prefix is stripped),
			// lower case it
			name[0] = (char) (((int) name[0]) + 32);
		}
		return new String(name);
	}

	@Override
	public MethodBuilder name(String methodName, Class<?> clazz) {
		BuilderPlugInUtil.errorIfNull(clazz, "Parameter type");

		Field[] allFieldList = clazz.getDeclaredFields();
		List<Field> nonStaticField = new ArrayList<>();
		for(Field f:allFieldList){
			if(!Modifier.isStatic(f.getModifiers())){
				nonStaticField.add(f);
			}
		}
		Field[] fieldList = new Field[nonStaticField.size()];
		nonStaticField.toArray(fieldList);
		
		if (fieldList != null && fieldList.length > 0) {

			String paramName = getParamName(clazz.getSimpleName());
			strSourceBuilder.append(methodName);
			strSourceBuilder.append(BRACKET_OPEN);
			strSourceBuilder.append(clazz.getSimpleName() + SPACE + paramName);
			strSourceBuilder.append(BRACKET_CLOSE).append(PARENTHESIS_OPEN_CHAR).append(NEW_LINE);
			strSourceBuilder.append("return new " + BUILDER + BRACKET_OPEN + BRACKET_CLOSE).append(NEW_LINE).append(DOT);

			for (Field field : fieldList) {
				
				MyField mf = new MyField(field, FunctionToRemovePrefix);
				strSourceBuilder.append(mf.getCanonicalName()).append(BRACKET_OPEN).append(paramName).append(DOT).append(mf.getOriginalName()).append(BRACKET_CLOSE);
				if(fieldList[fieldList.length-1] != field){
					strSourceBuilder.append(NEW_LINE).append(DOT);
				}
			}

		}
		strSourceBuilder.append(SEMI_COLON).append(NEW_LINE).append(PARENTHESIS_CLOSE_CHAR).append(NEW_LINE);

		return this;
	}

	public StringBuilder toSource() {
		return strSourceBuilder;
	}
}
