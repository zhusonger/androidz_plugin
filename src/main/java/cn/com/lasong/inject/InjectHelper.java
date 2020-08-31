package cn.com.lasong.inject;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import cn.com.lasong.utils.PluginHelper;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;

public class InjectHelper {

    //初始化类池
    protected final static ClassPool pool = ClassPool.getDefault();
    // 加入类路径的记录, 方便修改
    protected final static Map<String, ClassPath> clzPaths = new HashMap<>();

    /**
     * 添加javassist类搜索路径
     *
     * @param path
     * @return
     */
    public static boolean appendClassPath(String tag, String path) {
        removeClassPath(tag);
        try {
            ClassPath classPath = pool.appendClassPath(path);
            clzPaths.put(tag, classPath);
            return true;
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 移除tag对应的classpath
     *
     * @param tag
     */
    public static void removeClassPath(String tag) {
        ClassPath cachePath = clzPaths.get(tag);
        if (null != cachePath) {
            pool.removeClassPath(cachePath);
        }
    }

    /**
     * 处理三方的库
     *
     * @param group
     * @param input
     * @param extension
     * @param outputProvider
     * @param proDir
     * @throws IOException
     */
    public static void transformJar(String group, JarInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {
        //对jar文件进行处理
        // android.local.jars:agora-rtc-sdk.jar:d6d6fbc1426a002d1e92ffd161e4257c12816d32
        // android.local.jars:SenseArSourceManager-release-runtime.jar:12a9d3c4f421868d738d34abf42bbe7e83e4ff1c
        // cn.com.lasong:base:0.0.2
        String name = input.getName();
        String[] nameArr = name.split(":");
        String artifact = nameArr[1];
        boolean isLocalJar = artifact.endsWith(".jar");
        String tag = isLocalJar ? artifact : name;
        // build/tmp/xxx(任务名)
        File tmpDir = context.getTemporaryDir();

        File dstFile = outputProvider.getContentLocation(input.getName(), input.getContentTypes(),
                input.getScopes(), Format.JAR);

        File srcFile = input.getFile();


        if (null != extension) {
            Set<InjectDomain> set = extension.injectDomains;
            InjectDomain injectDomain = null;
            for (InjectDomain domain : set) {
                if (domain.group == null || domain.group.isEmpty()) {
                    continue;
                }
                // 需要注入的jarInput
                // 1. 非本地库 group完全匹配
                // 2. 本地库 group跟
                if ((!isLocalJar && name.equals(domain.group))
                        || (isLocalJar && name.contains(domain.group))) {
                    injectDomain = domain;
                    break;
                }
            }

            // 注入
            if (null != injectDomain) {

                // 添加jar包路径
                appendClassPath(tag, srcFile.getAbsolutePath());
                File srcTmpFile = new File(tmpDir, srcFile.getName());

                if (extension.injectDebug) {
                    PluginHelper.println(group, "========> Inject Begin transformJar [" + tag + "] <========");
                    PluginHelper.println(group, "name = " + name);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                }

                JarFile srcJar = new JarFile(srcFile);
                JarOutputStream srcJarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(srcTmpFile)));
                // 1. create class
                String clzNewDir = injectDomain.clzNewDir;
                if (null != clzNewDir && !clzNewDir.isEmpty()) {
                    File clzDir = new File(proDir, clzNewDir);
                    File dstDir = new File(tmpDir, clzNewDir);
                    // 先复制到临时目录
                    FileUtils.copyDirectory(clzDir, dstDir);
                    // 添加临时路径
                    appendClassPath(tag + "_clzNewDir", dstDir.getAbsolutePath());
                    // 移除缓存文件
                    cleanJavacCache(dstDir, srcFile);
                    // 修改字节码
                    writeClassToSource(group, injectDomain, extension.injectDebug, dstDir);
                    // 修改后的字节码加入到jar包
                    // 保留clzNewDir到classpath, 这样后面的修改就可以基于这个修改过的字节码
                    List<File> clzList = listClasses(dstDir);
                    String clzDirPath = dstDir.getAbsolutePath() + File.separator;
                    for (File file : clzList) {
                        String entryName = file.getAbsolutePath().replace(clzDirPath, "");
                        JarEntry dstEntry = new JarEntry(entryName);
                        srcJarOutput.putNextEntry(dstEntry);
                        byte[] buffer = IOUtils.toByteArray(new FileInputStream(file));
                        srcJarOutput.write(buffer);
                        srcJarOutput.closeEntry();
                    }
                }

                // 2. modify class
                Enumeration<JarEntry> entries = srcJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry srcEntry = entries.nextElement();
                    String entryName = srcEntry.getName();

                    JarEntry dstEntry = new JarEntry(entryName);
                    srcJarOutput.putNextEntry(dstEntry);
                    // 修改
                    byte[] buffer = injectClass(group, injectDomain, entryName);

                    // 如果没有修改, 使用原来的
                    if (null == buffer) {
                        buffer = IOUtils.toByteArray(srcJar.getInputStream(srcEntry));
                    }

                    srcJarOutput.write(buffer);
                    srcJarOutput.closeEntry();
                }


                srcJarOutput.close();
                srcJar.close();
                // 成功之后更新复制的源jar包
                srcFile = srcTmpFile;

                // 移除临时路径, 已经加入到jar包中
                removeClassPath(tag + "_clzNewDir");
                removeClassPath(tag);

                if (extension.injectDebug) {
                    PluginHelper.println(group, "");
                    PluginHelper.println(group, "name = " + name);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                    PluginHelper.println(group, "========> Inject End transformJar [" + tag + "] <========");
                    PluginHelper.println(group, "");
                }
            }
            // 否则没有需要注入的, 就直接复制, 不进行修改
        }
        // 写入到目标文件
        FileUtils.copyFile(srcFile, dstFile);

        // 更换jar包路径为最终修改后的jar包路径
        appendClassPath(tag, dstFile.getAbsolutePath());
    }

    /**
     * 处理自己项目的源码
     *
     * @param group
     * @param input
     * @param extension
     * @param outputProvider
     * @param context
     * @param proDir
     * @throws IOException
     */
    public static void transformCode(String group, DirectoryInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {

        //对源码文件进行处理
        String name = input.getName();
        // build/tmp/xxx(任务名)
        File tmpDir = context.getTemporaryDir();
        File dstFile = outputProvider.getContentLocation(input.getName(), input.getContentTypes(),
                input.getScopes(), Format.DIRECTORY);

        // 源码class目录
        File srcFile = input.getFile();

        if (null != extension) {
            Set<InjectDomain> set = extension.injectDomains;
            InjectDomain injectDomain = null;
            for (InjectDomain domain : set) {
                if (domain.group == null || domain.group.isEmpty()) {
                    continue;
                }
                // 当前库与注入目标库相同
                if (domain.group.equals(group)) {
                    injectDomain = domain;
                    break;
                }
            }

            if (null != injectDomain) {
                // 添加源码路径
                appendClassPath(group, srcFile.getAbsolutePath());

                if (extension.injectDebug) {
                    PluginHelper.println(group, "========> Inject Begin transformCode [" + group + "] <========");
                    PluginHelper.println(group, "group = " + group);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                }

                // 1. create class
                String clzNewDir = injectDomain.clzNewDir;
                File clzNewDstDir = null;
                if (null != clzNewDir && !clzNewDir.isEmpty()) {
                    File clzDir = new File(proDir, clzNewDir);
                    File dstDir = new File(tmpDir, clzNewDir);
                    // 先复制到临时目录
                    FileUtils.copyDirectory(clzDir, dstDir);
                    // 添加临时路径
                    appendClassPath(group + "_clzNewDir", dstDir.getAbsolutePath());
                    // 移除缓存文件
                    cleanJavacCache(dstDir, srcFile);
                    // 修改字节码
                    writeClassToSource(group, injectDomain, extension.injectDebug, dstDir);

                    clzNewDstDir = dstDir;
                }

                // 2. modify class
                List<File> clzList = listClasses(srcFile);
                String clzDirPath = srcFile.getAbsolutePath() + File.separator;
                for (File file : clzList) {
                    String entryName = file.getAbsolutePath().replace(clzDirPath, "");

                    // 修改
                    byte[] buffer = injectClass(group, injectDomain, entryName);

                    // 没有修改使用原始文件
                    if (null == buffer) {
                        buffer = IOUtils.toByteArray(new FileInputStream(file));
                    }

                    // 覆盖class文件
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(buffer);
                    fos.close();
                }

                // 复制到最终路径
                if (null != clzNewDstDir) {
                    FileUtils.copyDirectory(clzNewDstDir, srcFile);
                }
                // 移除临时路径, 已经拷贝的srcFile路径
                removeClassPath(group + "_clzNewDir");
                // 移除之前的源码路径
                removeClassPath(group);

                if (extension.injectDebug) {
                    PluginHelper.println(group, "");
                    PluginHelper.println(group, "group = " + group);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "========> Inject End transformCode [" + group + "] <========");
                    PluginHelper.println(group, "");
                }
            }
        }

        // 写入到目标文件
        FileUtils.copyDirectory(srcFile, dstFile);

        // 更换最终的源码路径
        appendClassPath(group, dstFile.getAbsolutePath());
    }

    /**
     * 移除javac中的缓存文件
     * @param clzDir  修改的字节码文件夹
     * @param srcFile 缓存的javac文件夹
     */
    private static void cleanJavacCache(File clzDir, File srcFile) {
        if (clzDir == null || null == srcFile) {
            return;
        }

        String clzDirPath = clzDir.getAbsolutePath() + File.separator;
        List<File> clzList = listClasses(clzDir);
        for (File file : clzList) {
            String entryName = file.getAbsolutePath().replace(clzDirPath, "");

            File srcCacheFile = new File(srcFile, entryName);

            // 移除之前缓存的文件
            if (srcCacheFile.exists()) {
                srcCacheFile.delete();
            }
        }

    }
    /**
     * 写入字节码到源码编译结果中
     * @param group
     * @param injectDomain
     * @param injectDebug
     * @param clzDir
     * @throws IOException
     */
    private static void writeClassToSource(String group, InjectDomain injectDomain, boolean injectDebug, File clzDir) throws IOException {
        if (clzDir == null) {
            return;
        }

        String clzDirPath = clzDir.getAbsolutePath() + File.separator;
        List<File> clzList = listClasses(clzDir);
        for (File file : clzList) {
            String entryName = file.getAbsolutePath().replace(clzDirPath, "");

            // 修改
            byte[] buffer = injectClass(group, injectDomain, entryName);

            // 没有修改使用原始文件
            if (null == buffer) {
                buffer = IOUtils.toByteArray(new FileInputStream(file));
            }

            // 写入class文件
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer);
            fos.close();
        }
    }

    /**
     * 注入类
     *
     * @param group
     * @param injectDomain
     * @param entryName
     * @return
     */
    public static byte[] injectClass(String group, InjectDomain injectDomain, String entryName) {
        InjectClzModify clzModify = null;
        if (null != injectDomain.clzModify && !injectDomain.clzModify.isEmpty()) {
            for (InjectClzModify modify : injectDomain.clzModify) {
                // 不注入就跳过
                if (!modify.isInject) {
                    continue;
                }
                String className = modify.className;

                String clzEntryName = modify.getEntryName();

                if (entryName.equals(clzEntryName)) {
                    URL url = pool.find(className);
                    if (null != url) {
                        // 找到修改的类
                        clzModify = modify;
                    }
                    break;
                }
            }
        }

        // 需要修改
        if (null != clzModify) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String className = clzModify.className;
            CtClass ctClass = null;

            try {
                ctClass = pool.get(className);
                // 解冻
                if (ctClass.isFrozen()) {
                    ctClass.defrost();
                }
                // 导入关联类
                List<String> importPackages = clzModify.importPackages;
                if (null != importPackages && !importPackages.isEmpty()) {
                    PluginHelper.println(group, "");
                    for (String packageName : importPackages) {
                        pool.importPackage(packageName);
                        PluginHelper.println(group, "importPackage [" + packageName + "]");
                    }
                }

                // 新增属性
                List<String> addFields = clzModify.addFields;
                if (null != addFields && !addFields.isEmpty()) {
                    PluginHelper.println(group, "");
                    for (String field : addFields) {
                        try {
                            CtField ctField = CtField.make(field, ctClass);
                            ctClass.addField(ctField);
                            PluginHelper.println(group, "addField [" + field + "]");
                        } catch (Exception e) {
                            PluginHelper.printlnErr(group, "addField [" + field + "] Failure!");
                            e.printStackTrace();
                        }
                    }
                }

                // 新增方法
                List<String> addMethods = clzModify.addMethods;
                if (null != addMethods && !addMethods.isEmpty()) {
                    PluginHelper.println(group, "");
                    for (String method : addMethods) {
                        try {
                            CtMethod ctMethod = CtMethod.make(method, ctClass);
                            ctClass.addMethod(ctMethod);
                            PluginHelper.println(group, "addMethod [" + method + "]");
                        } catch (Exception e) {
                            PluginHelper.printlnErr(group, "addMethod Failure!" + method);
                            e.printStackTrace();
                        }
                    }
                }

                // 修改方法
                List<InjectModifyMethod> modifyMethods = clzModify.modifyMethods;
                if (null != modifyMethods && !modifyMethods.isEmpty()) {
                    PluginHelper.println(group, "");
                    for (InjectModifyMethod method : modifyMethods) {
                        String name = method.name;
                        if (null == name || name.length() == 0) {
                            continue;
                        }
                        CtClass[] params = parseCtClass(method.params);

                        CtMethod ctMethod;
                        if (null != params) {
                            ctMethod = ctClass.getDeclaredMethod(name, params);
                        } else {
                            ctMethod = ctClass.getDeclaredMethod(name);
                        }

                        if (null == ctMethod) {
                            PluginHelper.printlnErr(group, "modifyMethod [" + name + "] name is null !");
                            continue;
                        }

                        String type = method.type;
                        if (null == type || type.length() == 0) {
                            PluginHelper.printlnErr(group, "modifyMethod [" + name + "] type is null !");
                            continue;
                        }
                        String content = method.content;
                        if (null == content || content.length() == 0) {
                            PluginHelper.printlnErr(group, "modifyMethod [" + name + "] content is null !");
                            continue;
                        }

                        if (type.equalsIgnoreCase("insertAt") && method.lineNum < 0) {
                            PluginHelper.printlnErr(group, "modifyMethod [" + name + "] lineNum can't empty !");
                            continue;
                        }

                        if (type.equalsIgnoreCase("insertBefore")) {
                            ctMethod.insertBefore(content);
                        } else if (type.equalsIgnoreCase("insertAfter")) {
                            ctMethod.insertAfter(content);
                        } else if (type.equalsIgnoreCase("insertAt")) {
                            ctMethod.insertAt(method.lineNum, content);
                        } else if (type.equalsIgnoreCase("setBody")) {
                            ctMethod.setBody(content);
                        }

                        String modifiers = method.modifiers;
                        if (null != modifiers && modifiers.length() > 0) {
                            int accessFlags;
                            if (modifiers.contains("public")) {
                                accessFlags = AccessFlag.PUBLIC;
                            } else if (modifiers.contains("private")) {
                                accessFlags = AccessFlag.PRIVATE;
                            } else {
                                accessFlags = AccessFlag.PROTECTED;
                            }
                            if (modifiers.contains("final")) {
                                accessFlags |= AccessFlag.FINAL;
                            }
                            if (modifiers.contains("static")) {
                                accessFlags |= AccessFlag.STATIC;
                            }
                            if (modifiers.contains("synchronized")) {
                                accessFlags |= AccessFlag.SYNCHRONIZED;
                            }
                            ctMethod.setModifiers(accessFlags);

                        }

                        PluginHelper.println(group, "modifyMethod [" + name + "] " + type);
                        PluginHelper.println(group, content);
                    }
                }
                bos.write(ctClass.toBytecode());
                PluginHelper.println(group, "modify [" + className + "] Done!");

                return bos.toByteArray();
            } catch (Exception e) {
                PluginHelper.printlnErr(group, "modify [" + className + "] Failure!");
                e.printStackTrace();
            } finally {
                if (null != ctClass) {
                    ctClass.detach();
                }

                // 移除导入的包
                List<String> importPackages = clzModify.importPackages;
                if (null != importPackages && !importPackages.isEmpty()) {
                    PluginHelper.println(group, "");
                    Set<String> pkgSet = new HashSet<>(importPackages);
                    Iterator<String> iterator = pool.getImportedPackages();
                    while (iterator.hasNext()) {
                        String pkg = iterator.next();
                        if (pkgSet.contains(pkg)) {
                            iterator.remove();
                            PluginHelper.println(group, "removePackage [" + pkg + "]");
                        }
                    }
                }

                //  修改完后不再进行注入
                clzModify.isInject = false;
            }
        }

        return null;
    }

    /**
     * 解析方法签名到CtClass参数
     *
     * @param params
     * @return
     */
    private static CtClass[] parseCtClass(String params) {
        CtClass[] ret = null;
        if (null != params && params.length() > 0) {
            try {
                ret = Descriptor.getParameterTypes(params, pool);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * 获取所有class文件
     *
     * @param dir
     * @return
     */
    private static List<File> listClasses(File dir) {
        File[] files = dir.listFiles();
        List<File> clzList = new ArrayList<>();
        if (null == files || files.length == 0) {
            return clzList;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                clzList.addAll(listClasses(file));
            } else {
                clzList.add(file);
            }
        }
        return clzList;
    }
}
