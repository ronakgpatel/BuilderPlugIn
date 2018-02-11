package pojobuilderplugin.builders;
import static pojobuilderplugin.util.BuilderPlugInConstants.BRACKET_CLOSE;
import static pojobuilderplugin.util.BuilderPlugInConstants.BRACKET_OPEN;
import static pojobuilderplugin.util.BuilderPlugInConstants.BUILDER;
import static pojobuilderplugin.util.BuilderPlugInConstants.EQUAL;
import static pojobuilderplugin.util.BuilderPlugInConstants.NEW_LINE;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_CLOSE;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_OPEN;
import static pojobuilderplugin.util.BuilderPlugInConstants.SEMI_COLON;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;

import pojobuilderplugin.util.BuilderPlugInConstants;
import pojobuilderplugin.util.BuilderPlugInUtil;
import pojobuilderplugin.wrapper.MyField;

public class ConstructorBuilder extends BaseBuilder {

	private String className;
	private String paramBuilderName;
	public ConstructorBuilder(String className){
		super();
		BuilderPlugInUtil.errorIfNullOrBlank(className, "Class name ");
		this.className = className;
	}

	@Override
	public ConstructorBuilder name(String name) {
		if(className.equals(name)){
			strSourceBuilder.append(className).append(BRACKET_OPEN).append(BUILDER);
			paramBuilderName = getParamName(BUILDER);
			addSpace();
			strSourceBuilder.append(new String(paramBuilderName));
			strSourceBuilder.append(BRACKET_CLOSE);
			return this;
		}
		throw new InvalidParameterException("Constructor name must match with class name");
	}

	private String getParamName(String builder) {
		char[] name = BUILDER.toCharArray();
		if ((int) name[0] < 96) {
			// if first character is caps(since the prefix is stripped),
			// lower case it
			name[0] = (char) (((int) name[0]) + 32);
		}
		return new String(name);
	}

	
	@Override
	public ConstructorBuilder fields(Field[] fieldNames) {
		strSourceBuilder.append(PARENTHESIS_OPEN);
		BuilderPlugInUtil.errorIfNullOrBlank(paramBuilderName,"Parameter name for constructor");
		if(fieldNames != null && fieldNames.length>0) {
			for(Field field : fieldNames) {
				if(!Modifier.isStatic(field.getModifiers())){
					MyField mf = new MyField(field, BuilderPlugInConstants.FunctionToRemovePrefix);
					strSourceBuilder.append(field.getName()).append(EQUAL).append(paramBuilderName + "."+mf.getActualName()).append(SEMI_COLON).append(NEW_LINE);
				}
			}
		}
		strSourceBuilder.append(PARENTHESIS_CLOSE);
		return this;
	}
	
	public StringBuilder toSource() {
		return strSourceBuilder;
	}
	
}
