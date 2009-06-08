package fiji.scripting;
//import ij.IJ;
//import ij.Menus;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
//import java.util.List;
import java.lang.reflect.*;
import java.lang.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.lang.Object;
import java.awt.List;
import java.awt.event.*;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.*;

	/****This class generates and prints the 
	list of trees having each part of the classnames path 
	as a node of the tree like for java.awt.List ,the top level List
	contains a Tree object with key "java" and one of its childList
	element as awt which in turn has its childDList having its one childList
	as Listwhich is infact also a leaf ***********/

class ClassNames {

	static List list=new List();

	static Package root=new Package();
	DefaultProvider defaultProvider;
	Enumeration list1;
	Package toReturnClassPart;
	public void run(String[] args) {

		for (int i = 1; i < args.length; i++){

			setPathTree(args[i]);
			 
		}

	}

	public Package getTopPackage() {
		return root;
	}




	public void setPathTree(String path){
		File file = new File(path);
		if (file.isDirectory()) {
			setDirTree(path);
		}
		if(path.endsWith(".jar")) {

			try {
				ZipFile jarFile = new ZipFile(file);
				list1 = jarFile.entries();
			}catch(Exception e){System.out.println("Invalid jar file");}
				while (list1.hasMoreElements()){
					ZipEntry entry =(ZipEntry)list1.nextElement();
						String name = entry.getName();
						if(!name.endsWith(".class"))		//ignoring the non class files
							continue;
						String[] classname1=name.split("\\\\");
						String justClassName=classname1[classname1.length-1];
						addToTree(justClassName,root,0);
				}

		}

	}

	public void setDirTree(String path) {
		File file = new File(path);
		if(file.isDirectory()) {
			if (!path.endsWith(File.separator))
				path += File.separator;
			String[] list = file.list();
			for (int i = 0; i < list.length; i++)
				setDirTree(path + list[i]);	//recursively adding the classnames to the list
		}

		if((path.endsWith(".class"))) {
			String[] classname1=path.split("\\\\");
			String justClassName=classname1[classname1.length-1];
			addToTree(justClassName,root,0);
		}
	}

	public void addToTree(String fullName,Package toAdd,int i) {
		String name=new String(fullName);                         //No splitting now
		//String[] classname2=justClassName.split("/");
		if(fullName.endsWith(".class")) {

			for(;;) {
				int slash=name.indexOf("/");
				if(slash<0) {
					break;
				}
				Package item = new Package(name.substring(0,slash)+".");
				toAdd.add(item);
				toAdd = (Package)toAdd.tailSet(item).first(); 
				name = name.substring(slash + 1);
			}
			Item item = new ClassName(name.substring(0,name.length()-6),fullName.substring(0,fullName.length()-6));

			toAdd.add(item);
			 
		}

	}





	public CompletionProvider getDefaultProvider(Package root,RSyntaxTextArea textArea) {
		defaultProvider=new DefaultProvider();

		String text=defaultProvider.getEnteredText(textArea);
		if(!(text=="" || text==null)) {
			String[] packageParts=new String[10];                             //this has to be improved as this restricts only less than 10 dots in a classfull name
			int index=text.lastIndexOf(".");
			if(index<0){
				Package packagePart = findItemSet(root,text);
				toReturnClassPart = new Package();
				Package classPart= findClassSet(root,text);
				packagePart.addAll(classPart);
				defaultProvider.addCompletions(createListCompletions(packagePart));
			}

			if(index>0) {
				classStartCompletions(text);
				String[] parts=text.split("\\.");
				boolean isDotAtLast=false;
				boolean isClassBeforeDot=false;
				boolean isPackageBeforeDot=false;
				index=parts.length;
				if(text.charAt(text.length()-1)=='.') {
					isDotAtLast=true;
				}
				Package temp=root;
				int temp1=index;
				packageParts=parts;
				Object temp2;
				boolean isPresent=true;
				while(temp1>1) {
					Item itemBeforeDot=findTailSet((Package)temp,packageParts[index-temp1]).first();
					if(itemBeforeDot instanceof ClassName) {
						//System.out.println(itemBeforeDot.getName()+" "+packageParts[index-temp1]);
						if(itemBeforeDot.getName().equals(packageParts[index-temp1])) {
							isClassBeforeDot=true;
						}
						else{
							isPresent=false;
						}
						break;                                                    //here I am assuming only one dot after a className that is why breaking the loop irrespective of isPresent
					}
					itemBeforeDot=findTailSet(temp,packageParts[index-temp1]+".").first();
					if(itemBeforeDot instanceof Package) {
						if(!((Package)findTailSet(temp,packageParts[index-temp1]+".").first()).getName().equals(packageParts[index-temp1]+".")) {//looks if topLevel contains the first part of the package part
							isPresent=false;
							break;
						}
						else{

								temp=(Package)findTailSet(temp,packageParts[index-temp1]+".").first();
							}
					}
					temp1--;

				}

				if(isPresent) {

					if(!isDotAtLast) {
						if(isClassBeforeDot) {
							ClassName name = (ClassName)findTailSet(temp,packageParts[index-temp1]).first();
							loadMethodNames(name);
							generateClassRelatedCompletions(name,packageParts);                   //does the rest of job when finds a class
						}
						else {
							temp=findItemSet(temp,packageParts[index-1]);
							defaultProvider.addCompletions(createListCompletions(temp));
						}
					}
					else  {
						Item temp3=findTailSet(temp,packageParts[index-temp1]).first();

						if(temp3 instanceof ClassName) {
							if(temp3.getName().equals(packageParts[index-temp1])) {
								//System.out.println(temp3.getName()+" "+packageParts[index-temp1]);
								loadMethodNames((ClassName)temp3);
								defaultProvider.addCompletions(createFunctionCompletion(((ClassName)temp3).methodNames));
							}
						}
						else {
							temp=(Package)temp3;
							defaultProvider.addCompletions(createListCompletions(temp));
						}

					}


				}
			}




		}
		return defaultProvider;

	}

	public void classStartCompletions(String text) {

			String[] classParts = text.split("\\.");
			boolean isDotAtLast=false;
			if(text.charAt(text.length()-1)=='.') {
				isDotAtLast=true;
			}
			if((classParts.length>1&&isDotAtLast) || (classParts.length>2&&!isDotAtLast)) {
				return;
			}
			Package classItemBeforeDot=findClassSet(root,classParts[0]);
			if(classItemBeforeDot.size()>0) {
				if(classItemBeforeDot.first().getName().equals(classParts[0])) {
					ClassName temp3=(ClassName)classItemBeforeDot.first();
					loadMethodNames(temp3);

					if(isDotAtLast) {
						defaultProvider.addCompletions(createFunctionCompletion(temp3.methodNames));
					}
					else {
						generateClassRelatedCompletions(temp3,classParts);
					}
				}
			}
	}


	public void loadMethodNames(ClassName temp) {
		if(temp.methodNames.size()<=0) {
			String fullname = temp.getCompleteName();
			try {
				try {
					Class clazz=getClass().getClassLoader().loadClass(fullname);
					temp.setMethodNames(clazz.getMethods());
					Field[] field = clazz.getFields();
					for (Field f : field)
						System.out.println(f.toString());
				} catch(java.lang.Error e) { e.printStackTrace(); }
			} catch(Exception e) { e.printStackTrace(); }
		}
	}

	public Package findTailSet(Package parent,String text) {
		Package item = new Package(text);
		Package tail=new Package();
		for(Item i : parent.tailSet(item)) {
			tail.add(i);
		}
		return tail;
	}

	public Package findHeadSet(Package parent,String text) {
		Package item = new Package(text);
		Package tail=new Package();
		for(Item i : parent.headSet(item)) {
			tail.add(i);
		}
		return tail;
	}

	public Package findItemSet(Package parent,String text) {
		Item item = new Package();
		Package toBeUsedInLoop=findTailSet(parent,text);
		System.out.println("the size of the tailset is"+toBeUsedInLoop.size());

		for(Item i: toBeUsedInLoop) {

			if(!(i.getName().startsWith(text))){
				item = i;
				break;
			}

			item=i;                                     
		}
		try {
			if(item.equals(toBeUsedInLoop.last())) {

				//System.out.println(((Tree)tree2).key);
				return(toBeUsedInLoop);
			}
			else {
				return(findHeadSet(toBeUsedInLoop,item.getName()));
			}
		} catch(Exception e){return toBeUsedInLoop;}
	}

	public Package findClassSet(Package parent,String text) {

		for(Item i : parent) {
			if(i instanceof ClassName) {
				if(i.getName().startsWith(text)) {
					toReturnClassPart.add(i);
				}
			}
			else {
				findClassSet((Package)i,text);
			}
		}
		return toReturnClassPart;

	}

	public ArrayList createListCompletions(Package setOfCompletions) {
		ArrayList listOfCompletions =new ArrayList();

		for(Item i : setOfCompletions) {
			try {
				try {
					if(i instanceof ClassName) {
						//Class clazz = Class.forName(((ClassName)i).getCompleteName());
						String fullName = ((ClassName)i).getCompleteName();
						Class clazz=getClass().getClassLoader().loadClass(fullName);
						Constructor[] ctor = clazz.getConstructors();

						for(Constructor c : ctor) {
							//System.out.println(c.toString());
							String cotrCompletion=createCotrCompletion(c.toString());
							listOfCompletions.add(new BasicCompletion(defaultProvider,cotrCompletion));
						}
						listOfCompletions.add(new BasicCompletion(defaultProvider,i.getName()+"."));
					}
				} catch(NoClassDefFoundError e){ e.printStackTrace(); }
			} catch(Exception e){ e.printStackTrace(); System.out.println(i.getName());}
			listOfCompletions.add(new BasicCompletion(defaultProvider,i.getName()));

		}
		System.out.println("the compltion list has "+listOfCompletions.size());
		return listOfCompletions;
	}

	public String createCotrCompletion(String cotr) {

		String[] bracketSeparated = cotr.split("\\(");
		int lastDotBeforeBracket = bracketSeparated[0].lastIndexOf(".");
		return(cotr.substring(lastDotBeforeBracket+1));

	}

	public ArrayList createFunctionCompletion(TreeSet<ClassMethod> setOfCompletions) {
		ArrayList listOfCompletions=new ArrayList();
		for(ClassMethod method : setOfCompletions) {
			if(method.isStatic && method.isPublic) {
				listOfCompletions.add(new FunctionCompletion(defaultProvider,method.onlyName,method.returnType));      //currently basiccompletion can be changed to functioncompletion
			}
		}
		return listOfCompletions;
	}

	public void generateClassRelatedCompletions(ClassName className,String[] parts) {
		//if(leftIndices==1) {
			TreeSet<ClassMethod> set =(TreeSet<ClassMethod>)className.methodNames.tailSet(new ClassMethod(parts[parts.length-1],true));
			for(ClassMethod c : set) {
				//System.out.println(s);
				if(!c.onlyName.startsWith(parts[parts.length-1])) {
					break;
				}
				else {
					if(c.isStatic && c.isPublic)
						defaultProvider.addCompletion(new FunctionCompletion(defaultProvider,c.onlyName,c.returnType));
				}
			}
		//}
	}




}




