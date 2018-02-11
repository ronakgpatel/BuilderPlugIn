
package pojobuilderplugin.handlers;

import static pojobuilderplugin.util.BuilderPlugInConstants.BLANK_STR;
import static pojobuilderplugin.util.BuilderPlugInConstants.BUILDER;
import static pojobuilderplugin.util.BuilderPlugInConstants.FunctionToRemovePrefix;
import static pojobuilderplugin.util.BuilderPlugInConstants.PACKAGE_KEYWORD;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_CLOSE_CHAR;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_OPEN_CHAR;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import net.openhft.compiler.CompilerUtils;
import pojobuilderplugin.builders.ClassBuilder;
import pojobuilderplugin.builders.ConstructorBuilder;
import pojobuilderplugin.builders.FieldBuilder;
import pojobuilderplugin.builders.MethodBuilder;
import pojobuilderplugin.exception.BuilderPlugInException;
import pojobuilderplugin.exception.NoFieldExistsException;
import pojobuilderplugin.util.BuilderPlugInConstants;
import pojobuilderplugin.util.BuilderPlugInUtil;
import pojobuilderplugin.util.Function;
import pojobuilderplugin.wrapper.MyField;

public class CreateBuilderHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			boolean b=showConfirmWindow(event, "Do you want to generate builder class ?");
            if(b)
            {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			BuilderPlugInUtil.errorIfNull(workspace, "Workspace Object");

			IWorkspaceRoot root = workspace.getRoot();
			BuilderPlugInUtil.errorIfNull(root, "Workspace Root");

			IWorkbench wb = PlatformUI.getWorkbench();
			BuilderPlugInUtil.errorIfNull(wb, "Workbench Object");

			IWorkbenchWindow window1 = wb.getActiveWorkbenchWindow();
			BuilderPlugInUtil.errorIfNull(window1, "Window Object");

			IWorkbenchPage page = window1.getActivePage();
			BuilderPlugInUtil.errorIfNull(page, "Page Object");

			IEditorPart editor = page.getActiveEditor();
			BuilderPlugInUtil.errorIfNull(editor, "Editor Object");

			IEditorInput input = editor.getEditorInput();
			BuilderPlugInUtil.errorIfNull(input, "Editor Input Object");

			IPath path = ((IPathEditorInput) input).getPath();
			BuilderPlugInUtil.errorIfNull(path, "File path ");

			final File file = Paths.get(path.toString()).toFile();
			final String publicClassName = file.getName();
			final String classNameWithOutExt = publicClassName.substring(0, publicClassName.indexOf("."));
			final String fileString = new String(Files.readAllBytes(Paths.get(path.toString())),
					StandardCharsets.UTF_8);
			final String pkgName = getPackageName(fileString);

			// Get all projects in the workspace
			final IProject[] projects = root.getProjects();
			BuilderPlugInUtil.errorIfNull(projects, "Projects");

			final List<IJavaProject> projectList = new ArrayList<>();
			IJavaProject currentProject = null;

			// Loop over all projects
			currentProject = BuilderPlugInUtil.addAllProjectsAndReturnCurrentProjectName(path, projects, projectList);
			String projectDirPath = BuilderPlugInUtil.getProjectDirPath(path.toString(), pkgName);
			final Set<URL> urlList = BuilderPlugInUtil.getAllClasspathEntriesAsUrls(currentProject, projectDirPath);

			// add bin to path
			String projectBuildPath = BuilderPlugInUtil.convertToBuildPath(projectDirPath);
			urlList.add(new File(projectBuildPath).toURI().toURL());

			final URL[] urls = new URL[urlList.size()];
			urlList.toArray(urls);
			
			Class<?> aClass = CompilerUtils.CACHED_COMPILER.loadFromJava(pkgName.trim() + classNameWithOutExt,
					fileString, urls);

			char[] fileContent = fileString.toCharArray();
			fileContent = removeExistingBuilerCode(aClass, fileContent);

			// String builderClassName = classNameWithOutExt +
			// BUILDER_CLASS_SUFFIX;

			// create constructor in the pojo class with Builder arg\
			// .fields() contains the list of fields to be initialized inside
			// the constructor body
			/**
			 * private <ClassName> (Builder builder) { <<FIELDS>> }
			 */
			StringBuilder strBuilderConstructorPojo = new ConstructorBuilder(aClass.getSimpleName()).setPrivate()
					.name(aClass.getSimpleName()).fields(aClass.getDeclaredFields()).toSource();

			// create copy builder method - static code for all classes
			/**
			 * public static Builder builder(<<ClassName>> myClass) { return new
			 * Builder().value(myClass.myValue).listString(myClass.myListString);
			 * Suggested by Erik - 2016.11.11 }
			 */
			StringBuilder strCopyBuilderMethodInPojo = new MethodBuilder().setPublic().setStatic().returns(BUILDER)
					.name("builder", aClass).toSource();

			// create method - static code for all classes
			/**
			 * public static Builder builder(){ return new Builder(); }
			 */
			StringBuilder strBuilderMethodInPojo = new MethodBuilder().setPublic().setStatic().returns(BUILDER)
					.name("builder").code("return new " + BUILDER + "();").toSource();

			// prefix value is used to strip from field name to derive the param
			// name for field. for e.g myValue would become value. i.e
			// myValue=value;
			StringBuilder sourceStr = generateBuilderClass(BUILDER, aClass);

			// append all the generated source(methods) & inner class
			// into the string for original file content.
			strBuilderMethodInPojo.append(strBuilderConstructorPojo).append(strCopyBuilderMethodInPojo)
					.append(sourceStr);

			char[] updatedContent = insertGeneratedBuilderClass(fileContent,
					strBuilderMethodInPojo.toString().toCharArray());

			// append the content in the original source code file.
			Files.write(Paths.get(path.toString()), String.valueOf(updatedContent).getBytes("UTF-8"));
			Robot robot = new Robot();
			robot.keyPress(KeyEvent.VK_F5);
            }
		} catch (BuilderPlugInException pex) {
			showMessageWindow(event, pex.getMessage());
			pex.printStackTrace();
		} catch (Throwable e) {
			showMessageWindow(event, "Builder class could not be generated due to unexpected error");
			e.printStackTrace();
		}

		return null;
		
		
	}

	private char[] removeExistingBuilerCode(Class<?> aClass, char[] fileContent) throws BuilderPlugInException {
		fileContent = ifBuilderConstructorExistsThenRemove(BUILDER, aClass, fileContent);
		int currLen = fileContent.length;
		fileContent = ifBuilderMethodExistsThenRemove(BUILDER, aClass, fileContent);
		while (currLen != fileContent.length) {
			// looping is to tackle removal of multiple builder methods
			// in the pojo class, all must be removed
			currLen = fileContent.length;
			fileContent = ifBuilderMethodExistsThenRemove(BUILDER, aClass, fileContent);
		}
		fileContent = ifBuilderClassExistsThenRemove(BUILDER, aClass, fileContent);
		return fileContent;
	}

	private static char[] insertGeneratedBuilderClass(char[] fileContent, char[] generatedContent) {
		char[] updatedContent = new char[fileContent.length + generatedContent.length];

		for (int i = 0; i < updatedContent.length; i++) {
			updatedContent[i] = ' ';
		}
		int insertAt = -1;
		for (int idx = fileContent.length - 1; idx > 0; idx--) {
			if (fileContent[idx] == BuilderPlugInConstants.PARENTHESIS_CLOSE_CHAR) {
				insertAt = idx;
				break;
			}
		}

		if (insertAt > 0) {
			for (int idx = 0; idx < insertAt - 1; idx++) {
				updatedContent[idx] = fileContent[idx];
			}
			int len = insertAt - 1;
			for (int idx = 0; idx < generatedContent.length; idx++) {
				updatedContent[len + idx] = generatedContent[idx];
			}
			len = updatedContent.length - 1;
			updatedContent[len] = BuilderPlugInConstants.PARENTHESIS_CLOSE_CHAR;
		}
		return updatedContent;
	}

	private void showMessageWindow(ExecutionEvent event, String message) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "Generate Builder", message);
	}
	private boolean showConfirmWindow(ExecutionEvent event, String message) throws ExecutionException{
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		return MessageDialog.openQuestion(null, "Generate Builder", message);
	}

	private static StringBuilder generateBuilderClass(String builderClassName, Class<?> clazz)
			throws NoFieldExistsException {

		Field[] allFields = clazz.getDeclaredFields();
		if (allFields != null) {

			MyField[] allMyFields = new MyField[allFields.length];
			int indx = 0;
			for (Field f : allFields) {
				if(!Modifier.isStatic(f.getModifiers())){
				MyField mf = new MyField(f, FunctionToRemovePrefix);
				allMyFields[indx] = mf;
				indx++;
				}
			}

			StringBuilder classCode = new ClassBuilder().setPublic().setStatic().name(builderClassName)
					.fields(allMyFields).mutators(allMyFields).buildMethod(clazz).toSource();

			return classCode;
			
		} else {
			throw new NoFieldExistsException("Can not generate builder class as no instance fields defined");
		}
	}

	private String getPackageName(String fileString) {
		if (BuilderPlugInUtil.isNullOrBlank(fileString)) {
			return BLANK_STR;
		}
		if (fileString.indexOf(PACKAGE_KEYWORD) == -1)
			return "";

		int indx = fileString.indexOf(PACKAGE_KEYWORD) + PACKAGE_KEYWORD.length();
		int endIndx = fileString.indexOf(BuilderPlugInConstants.SEMI_COLON, indx);
		return fileString.substring(indx, endIndx) + ".";

	}

	private static char[] ifBuilderConstructorExistsThenRemove(String className, Class<?> cl, char[] arrData)
			throws BuilderPlugInException {
		if (arrData == null || arrData.length < 0 || cl == null) {
			throw new BuilderPlugInException("Source class can not be null");
		}
		int startIndex = -1;
		int endIndex = -1;
		String fileString = String.valueOf(arrData);
		// need to find the constructor and remove it.
		Constructor<?>[] constructors = cl.getDeclaredConstructors();
		if (constructors != null && constructors.length > 0) {
			for (Constructor<?> c : constructors) {
				Class<?>[] argTypes = c.getParameterTypes();
				if (argTypes != null && argTypes.length > 0) {
					for (Class<?> clazz : argTypes) {
						if (BUILDER.equals(clazz.getSimpleName())) {
							StringBuilder strBuilder = new ConstructorBuilder(cl.getSimpleName()).setPrivate()
									.name(cl.getSimpleName()).toSource();
							startIndex = fileString.indexOf(strBuilder.toString());
							Stack<Character> stack = new Stack<>();
							// now find the end of the method
							for (int i = startIndex + strBuilder.length(); i < arrData.length; i++) {
								if (arrData[i] == ' ')
									continue;
								if (arrData[i] == PARENTHESIS_OPEN_CHAR)
									stack.push(PARENTHESIS_OPEN_CHAR);
								else if (arrData[i] == PARENTHESIS_CLOSE_CHAR)
									stack.pop();
								if (stack.isEmpty()) {
									// now done with the method, exit
									endIndex = ++i;
									break;
								}
							}
						}
					}
				}
			}
		}
		return removeFromArray(arrData, new ArrayIndex(startIndex, endIndex));
	}

	private static char[] ifBuilderMethodExistsThenRemove(String className, Class<?> cl, char[] arrData)
			throws BuilderPlugInException {
		if (arrData == null || arrData.length < 0 || cl == null) {
			throw new BuilderPlugInException("Source class can not be null");
		}
		int startIndex = -1;
		int endIndex = -1;
		String fileString = String.valueOf(arrData);
		try {
			Method builderMethod = cl.getDeclaredMethod("builder");
			if (builderMethod != null) {
				StringBuilder strBuilder = new MethodBuilder().setPublic().setStatic().returns(BUILDER).name("builder")
						.toSource();

				startIndex = fileString.indexOf(strBuilder.toString().replace(")", ""));
				Stack<Character> stack = new Stack<>();
				// now find the end of the method
				for (int i = startIndex + strBuilder.length() + 1; i < arrData.length; i++) {
					if (arrData[i] == ' ')
						continue;
					if (arrData[i] == PARENTHESIS_OPEN_CHAR)
						stack.push(PARENTHESIS_OPEN_CHAR);
					else if (stack.isEmpty())
						continue;
					else if (arrData[i] == PARENTHESIS_CLOSE_CHAR)
						stack.pop();
					if (stack.isEmpty()) {
						// now done with the method, exit
						endIndex = ++i;
						break;
					}
				}
			}
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {

		}
		return removeFromArray(arrData, new ArrayIndex(startIndex, endIndex));
	}

	private static char[] ifBuilderClassExistsThenRemove(String className, Class<?> cl, char[] arrData)
			throws BuilderPlugInException {

		int startIndex = -1;
		int endIndex = -1;
		if (arrData == null || arrData.length < 0 || cl == null) {
			throw new BuilderPlugInException("Source class can not be null");
		}

		String fileString = String.valueOf(arrData);
		Class<?>[] innerClasses = cl.getDeclaredClasses();
		if (innerClasses != null) {
			for (Class<?> c : innerClasses) {
				if (c.getSimpleName().equals(className)) {
					// class exists hence remove it from source code
					String strClassToMatch = new ClassBuilder().setPublic().setStatic().name(className).toString()
							.replaceAll("\n", "");
					startIndex = fileString.indexOf(strClassToMatch);
					Stack<Character> stack = new Stack<>();
					// now find the end of the class
					for (int i = startIndex + strClassToMatch.length() - 1; i < arrData.length; i++) {
						if (arrData[i] == ' ')
							continue;
						if (arrData[i] == PARENTHESIS_OPEN_CHAR)
							stack.push(PARENTHESIS_OPEN_CHAR);
						else if (arrData[i] == PARENTHESIS_CLOSE_CHAR)
							stack.pop();
						if (stack.isEmpty()) {
							// now done with the class
							endIndex = ++i;
							break;
						}
					}
					break;
				}
			}
		}
		return removeFromArray(arrData, new ArrayIndex(startIndex, endIndex));
		// return removeFromArray(new ArrayIndex(startIndex, endIndex),
		// arrData);
	}

	private static char[] removeFromArray(char[] arrData, ArrayIndex... aI) {

		int totalLen = 0;
		if (aI != null && aI.length > 0) {
			for (ArrayIndex a : aI) {
				if (a.startIndex > 0) {
					totalLen += a.endIndex - a.startIndex;
				}
			}
		}
		if (totalLen > 0) {
			char[] removedBuilderMethodStr = new char[arrData.length - (totalLen)];
			for (ArrayIndex a : aI) {
				if (a.startIndex > 0) {
					for (int i = 0; i < a.startIndex; i++) {
						removedBuilderMethodStr[i] = arrData[i];
					}

					for (int idx = a.startIndex, eIndx = a.endIndex; eIndx < arrData.length; idx++, eIndx++) {
						removedBuilderMethodStr[idx] = arrData[eIndx];
					}

				}
			}

			return removedBuilderMethodStr;

		} else
			return arrData;
	}

	private static class ArrayIndex {

		int startIndex;
		int endIndex;

		ArrayIndex(int sI, int eI) {
			this.startIndex = sI;
			this.endIndex = eI;
		}
	}

	static void test() {

		try {

			String fileString = new String(
					Files.readAllBytes(Paths
							.get("C:\\Users\\eropate\\runtime-EclipseApplication\\Test\\src\\com\\test\\Test.java")),
					StandardCharsets.UTF_8);

			Class<?> aClass = CompilerUtils.CACHED_COMPILER.loadFromJava("com.test.Test", fileString);
			char[] fileContent = fileString.toCharArray();
			ifBuilderClassExistsThenRemove("Test", aClass, fileContent);
			StringBuilder sourceStr = generateBuilderClass("Test" + BUILDER, aClass);

			char[] generatedContent = sourceStr.toString().toCharArray();
			char[] updatedContent = insertGeneratedBuilderClass(fileContent, generatedContent);
			System.out.println(String.valueOf(updatedContent));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// test();
		try {

			class JunitTest {
				Object myValue;
				List<String> myListOfValues;
			}

			Field myValueField = JunitTest.class.getDeclaredField("myValue");
			Field myListField = JunitTest.class.getDeclaredField("myListOfValues");
			testAppendMethodDeclaration(BUILDER, new StringBuilder(),
					new MyField(myListField, new Function<Field, String>() {

						@Override
						public String apply(Field t1) {
							return "listOfValues";
						}
					}));
			testAppendFieldDeclaration(new StringBuilder(), new MyField(myValueField, new Function<Field, String>() {

				@Override
				public String apply(Field t1) {
					return "value";
				}
			}));
			testClassGeneration(JunitTest.class, BUILDER,
					new MyField[] { new MyField(myValueField, new Function<Field, String>() {

						@Override
						public String apply(Field t1) {
							return "value";
						}
					}), new MyField(myListField, new Function<Field, String>() {

						@Override
						public String apply(Field t1) {
							return "listOfValues";
						}
					}) });

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void testClassGeneration(Class<?> clazz, String className, MyField[] allMyFields) {
		StringBuilder classCode = new ClassBuilder().setPublic().setStatic().name(className).fields(allMyFields)
				.mutators(allMyFields).buildMethod(clazz).toSource();
		System.out.println("***Gen Code *** : " + classCode);

	}

	public static void testAppendFieldDeclaration(StringBuilder builderStr, MyField f) {
		StringBuilder strBuilder = new FieldBuilder().setPrivate().type(f.getType()).name(f.getActualName()).toSource();
		builderStr.append(strBuilder);
		System.out.println(builderStr);
	}

	public static void testAppendMethodDeclaration(String clazz, StringBuilder builderStr, MyField f) {
		builderStr.append(BuilderPlugInConstants.NEW_LINE);

		// generates something like this - noprefix for method name
		/**
		 * public Builder str(String str){ myStr = str; return this; }
		 */

		StringBuilder strMethodCode = new MethodBuilder().setPublic().returns(clazz).name(f.getCanonicalName(), f)
				.toSource();
		builderStr.append(strMethodCode);
		System.out.println(builderStr);
	}

}
