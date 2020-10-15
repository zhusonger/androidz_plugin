package cn.com.lasong.inject;

public class InjectCtModify {

    public static final String ACTION_MODIFY = "MODIFY";
    public static final String ACTION_ADD_FIELD = "ADD_FIELD";
    public static final String ACTION_ADD_METHOD = "ADD_METHOD";
    public static final String ACTION_DEFAULT = ACTION_MODIFY;
    public String action = ACTION_DEFAULT;
    // 修饰符
    public String modifiers;

    // 方法名
    public String name;

    // 是否是修改构造方法
    public boolean isConstructor;

    // 方法参数
    public String params;

    // 变量名
    public String fieldName;

    // 新变量名
    public String newFieldName;

    // 属性修饰符
    public String fieldModifiers;

    // 插入的代码
    public String content;

    // 新的方法名
    public String newName;

    // 插入类型
    // insertBefore : 在方法的起始位置插入代码；
    // insertAfter : 在方法的所有 return 语句前插入代码以确保语句能够被执行，除非遇到exception；
    // insertAt : 在指定的位置插入代码；
    // setBody : 将方法的内容设置为要写入的代码，当方法被 abstract修饰时，该修饰符被移除；
    // deleteAt: 在指定的位置删除代码
    public String type;

    // 行号, insertAt用到
    public int lineNum = -1;

    // 删除行用到
    // 起始行 相对于方法, 第一行是0
    // 起始行0#删除行数0,起始行1#删除行数2
    // 不加删除行数, 默认一行
    // 起始行0,起始行1 = 起始行0#1,起始行1#1
    public String lineRange;

    public String getModifiers() {
        return modifiers;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setModifiers(String modifiers) {
        this.modifiers = modifiers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getLineRange() {
        return lineRange;
    }

    public void setLineRange(String lineRange) {
        this.lineRange = lineRange;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getNewFieldName() {
        return newFieldName;
    }

    public void setNewFieldName(String newFieldName) {
        this.newFieldName = newFieldName;
    }

    public String getFieldModifiers() {
        return fieldModifiers;
    }

    public void setFieldModifiers(String fieldModifiers) {
        this.fieldModifiers = fieldModifiers;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public void setConstructor(boolean constructor) {
        isConstructor = constructor;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"action\":\"")
                .append(action).append('\"');
        sb.append(",\"modifiers\":\"")
                .append(modifiers).append('\"');
        sb.append(",\"name\":\"")
                .append(name).append('\"');
        sb.append(",\"isConstructor\":")
                .append(isConstructor);
        sb.append(",\"params\":\"")
                .append(params).append('\"');
        sb.append(",\"fieldName\":\"")
                .append(fieldName).append('\"');
        sb.append(",\"newFieldName\":\"")
                .append(newFieldName).append('\"');
        sb.append(",\"fieldModifiers\":\"")
                .append(fieldModifiers).append('\"');
        sb.append(",\"content\":\"")
                .append(content).append('\"');
        sb.append(",\"newName\":\"")
                .append(newName).append('\"');
        sb.append(",\"type\":\"")
                .append(type).append('\"');
        sb.append(",\"lineNum\":")
                .append(lineNum);
        sb.append(",\"lineRange\":\"")
                .append(lineRange).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
