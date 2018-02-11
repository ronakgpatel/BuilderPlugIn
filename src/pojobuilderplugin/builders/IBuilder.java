package pojobuilderplugin.builders;

import java.lang.reflect.Field;

import pojobuilderplugin.wrapper.MyField;

public interface IBuilder {

	void addSpace();

	IBuilder setPublic();

	IBuilder setProtected();

	IBuilder setPrivate();

	IBuilder setStatic();

	IBuilder returns(String returnType);

	IBuilder name(String name);

	IBuilder name(String name, MyField mf);

	IBuilder name(String name, Class<?> clazz);
	
	IBuilder code(String strSourceCode);

	IBuilder fields(Field[] fieldNames);
	
	IBuilder mutators(MyField[] fieldNames);
	
	IBuilder fields(MyField[] fieldNames);

	IBuilder type(String strType);
	
	IBuilder buildMethod(Class<?> clazz);

	StringBuilder toSource();
}
