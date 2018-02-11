package pojobuilderplugin.wrapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.security.InvalidParameterException;

import pojobuilderplugin.util.BuilderPlugInConstants;
import pojobuilderplugin.util.Function;

public class MyField {

	final private Field _field;
	final private Function<Field, String> _functionToGetCanonicalName;
	final private String _getParamName;
	final private String _canonicalName;
	final private String _type;
	private String _fieldName;

	public MyField(Field field, Function<Field, String> function) {
		_field = field;
		_fieldName = _field.getName();
		_functionToGetCanonicalName = function;
		if (_field == null)
			throw new InvalidParameterException("Field name can not be null");

		if (_functionToGetCanonicalName != null) {
			_getParamName = function.apply(_field);
		} else {
			_getParamName = _fieldName;
		}
		
		//if field doesn't have 'my' prefix then we must add that
		//or paramName & fieldName are same then we must put prefix for field name
		if(_getParamName.equals(_fieldName)) {
			char[] fName = _fieldName.toCharArray();
			if((int)fName[0] > 96){
				//if char is lower case, do upper case it
				fName[0]=(char)(((int) fName[0]) - 32);
			}
			_fieldName = BuilderPlugInConstants.FIELD_NAME_PREFIX + String.valueOf(fName);
		}

		char[] name = _getParamName.toCharArray();
		if ((int) name[0] < 96) {
			// if first character is caps(since the prefix is stripped),
			// lower case it
			name[0] = (char) (((int) name[0]) + 32);
		}

		_canonicalName = String.valueOf(name);
		
		String gType = _field.getType().getSimpleName();
		try {
			ParameterizedType genericType = (ParameterizedType) _field.getGenericType();
			gType = genericType.toString();
		} catch (Exception nx) {
			// ignore
			gType = _field.getType().getSimpleName();
		}
		_type = gType;

		
	}
	
	public String getOriginalName(){
		return _field.getName();
	}

	public String getActualName() {
		return _fieldName;
	}

	public String getParamName() {
		return _getParamName;
	}

	public String getCanonicalName(){
		return _canonicalName;
	}
	public String getType(){
		return _type;
	}
}
