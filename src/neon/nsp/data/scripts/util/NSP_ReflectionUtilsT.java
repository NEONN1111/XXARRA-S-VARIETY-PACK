package neon.nsp.data.scripts.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/*
Made by Lukas04, improved with help from Starficz.
Concepts have been learned from lyravega, float, andylizi and their mods.
**/
public class NSP_ReflectionUtilsT {
    
    private static final Class<?> fieldClass;
    private static final java.lang.invoke.MethodHandle setFieldHandle;
    private static final java.lang.invoke.MethodHandle getFieldHandle;
    private static final java.lang.invoke.MethodHandle getFieldTypeHandle;
    private static final java.lang.invoke.MethodHandle getFieldNameHandle;
    private static final java.lang.invoke.MethodHandle setFieldAccessibleHandle;
    
    private static final Class<?> methodClass;
    private static final java.lang.invoke.MethodHandle getMethodNameHandle;
    private static final java.lang.invoke.MethodHandle getMethodParametersHandle;
    private static final java.lang.invoke.MethodHandle getMethodReturnTypeHandle;
    private static final java.lang.invoke.MethodHandle invokeMethodHandle;
    
    // Cache
    private static final HashMap<ReflectedFieldKey, ReflectedField> fieldsCache = new HashMap<>();
    private static final HashMap<Class<?>, HashSet<String>> fieldNameCache = new HashMap<>();
    private static final HashMap<Class<?>, HashSet<Class<?>>> fieldTypesCache = new HashMap<>();
    
    private static final HashMap<ReflectedMethodKey, ReflectedMethod> methodsCache = new HashMap<>();
    private static final HashMap<Class<?>, HashSet<String>> methodNameCache = new HashMap<>();
    
    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            setFieldHandle = MethodHandles.lookup().findVirtual(
                fieldClass, 
                "set", 
                MethodType.methodType(void.class, Object.class, Object.class)
            );
            getFieldHandle = MethodHandles.lookup().findVirtual(
                fieldClass, 
                "get", 
                MethodType.methodType(Object.class, Object.class)
            );
            getFieldTypeHandle = MethodHandles.lookup().findVirtual(
                fieldClass, 
                "getType", 
                MethodType.methodType(Class.class)
            );
            getFieldNameHandle = MethodHandles.lookup().findVirtual(
                fieldClass, 
                "getName", 
                MethodType.methodType(String.class)
            );
            setFieldAccessibleHandle = MethodHandles.lookup().findVirtual(
                fieldClass,
                "setAccessible",
                MethodType.methodType(void.class, boolean.class)
            );
            
            methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
            getMethodNameHandle = MethodHandles.lookup().findVirtual(
                methodClass, 
                "getName", 
                MethodType.methodType(String.class)
            );
            getMethodParametersHandle = MethodHandles.lookup().findVirtual(
                methodClass, 
                "getParameterTypes", 
                MethodType.methodType(Class[].class)
            );
            getMethodReturnTypeHandle = MethodHandles.lookup().findVirtual(
                methodClass, 
                "getReturnType", 
                MethodType.methodType(Class.class)
            );
            invokeMethodHandle = MethodHandles.lookup().findVirtual(
                methodClass, 
                "invoke", 
                MethodType.methodType(Object.class, Object.class, Object[].class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ReflectionUtilsT", e);
        }
    }
    
    // Cached Results of Reflected Fields
    private static class ReflectedFieldKey {
        final Class<?> clazz;
        final String fieldName;
        final Class<?> fieldType;
        final boolean withSuper;
        
        ReflectedFieldKey(Class<?> clazz, String fieldName, Class<?> fieldType, boolean withSuper) {
            this.clazz = clazz;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.withSuper = withSuper;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReflectedFieldKey that = (ReflectedFieldKey) o;
            return withSuper == that.withSuper &&
                   Objects.equals(clazz, that.clazz) &&
                   Objects.equals(fieldName, that.fieldName) &&
                   Objects.equals(fieldType, that.fieldType);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(clazz, fieldName, fieldType, withSuper);
        }
    }
    
    public static class ReflectedField {
        private final Object field;
        
        ReflectedField(Object field) {
            this.field = field;
        }
        
        public Object get(Object instance) throws Throwable {
            return getFieldHandle.invoke(field, instance);
        }
        
        public void set(Object instance, Object value) throws Throwable {
            setFieldHandle.invoke(field, instance, value);
        }
    }
    
    // Cached Results of Reflected Methods
    private static class ReflectedMethodKey {
        final Class<?> clazz;
        final String methodName;
        final Integer numOfParams;
        final Class<?> returnType;
        final List<Class<?>> parameterTypes;
        
        ReflectedMethodKey(Class<?> clazz, String methodName, Integer numOfParams, Class<?> returnType, List<Class<?>> parameterTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.numOfParams = numOfParams;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReflectedMethodKey that = (ReflectedMethodKey) o;
            return Objects.equals(clazz, that.clazz) &&
                   Objects.equals(methodName, that.methodName) &&
                   Objects.equals(numOfParams, that.numOfParams) &&
                   Objects.equals(returnType, that.returnType) &&
                   Objects.equals(parameterTypes, that.parameterTypes);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(clazz, methodName, numOfParams, returnType, parameterTypes);
        }
    }
    
    public static class ReflectedMethod {
        private final Object method;
        
        ReflectedMethod(Object method) {
            this.method = method;
        }
        
        public Object invoke(Object instance, Object... arguments) throws Throwable {
            return invokeMethodHandle.invoke(method, instance, arguments);
        }
    }
    
    public static void set(String fieldName, Object instanceToModify, Object newValue) throws Throwable {
        set(fieldName, instanceToModify, newValue, null);
    }
    
    public static void set(String fieldName, Object instanceToModify, Object newValue, Class<?> fieldType) throws Throwable {
        getField(fieldName, instanceToModify.getClass(), fieldType, null, null).set(instanceToModify, newValue);
    }
    
    public static Object get(String fieldName, Object instanceToGetFrom) throws Throwable {
        return get(fieldName, instanceToGetFrom, null);
    }
    
    public static Object get(String fieldName, Object instanceToGetFrom, Class<?> fieldType) throws Throwable {
        return getField(fieldName, instanceToGetFrom.getClass(), fieldType, null, null).get(instanceToGetFrom);
    }
    
    public static Object instantiate(Class<?> clazz, Object... arguments) throws Throwable {
        List<Class<?>> args = new ArrayList<>();
        for (Object arg : arguments) {
            if (arg == null) {
                args.add(Object.class);
            } else {
                Class<?> argClass = arg.getClass();
                // Check for primitive types
                if (argClass == Integer.class) args.add(int.class);
                else if (argClass == Boolean.class) args.add(boolean.class);
                else if (argClass == Byte.class) args.add(byte.class);
                else if (argClass == Character.class) args.add(char.class);
                else if (argClass == Short.class) args.add(short.class);
                else if (argClass == Long.class) args.add(long.class);
                else if (argClass == Float.class) args.add(float.class);
                else if (argClass == Double.class) args.add(double.class);
                else args.add(argClass);
            }
        }
        
        MethodType methodType = MethodType.methodType(void.class, args.toArray(new Class<?>[0]));
        java.lang.invoke.MethodHandle constructorHandle = MethodHandles.lookup().findConstructor(clazz, methodType);
        return constructorHandle.invokeWithArguments(Arrays.asList(arguments));
    }
    
    public static Object invoke(String methodName, Object instance, Object... arguments) throws Throwable {
        return invoke(methodName, instance, null, null, arguments);
    }
    
    public static Object invoke(String methodName, Object instance, Class<?> returnType, Integer parameterCount, Object... arguments) throws Throwable {
        Class<?> clazz = instance.getClass();
        List<Class<?>> args = new ArrayList<>();
        for (Object arg : arguments) {
            if (arg == null) {
                args.add(Object.class);
            } else {
                Class<?> argClass = arg.getClass();
                // Check for primitive types
                if (argClass == Integer.class) args.add(int.class);
                else if (argClass == Boolean.class) args.add(boolean.class);
                else if (argClass == Byte.class) args.add(byte.class);
                else if (argClass == Character.class) args.add(char.class);
                else if (argClass == Short.class) args.add(short.class);
                else if (argClass == Long.class) args.add(long.class);
                else if (argClass == Float.class) args.add(float.class);
                else if (argClass == Double.class) args.add(double.class);
                else args.add(argClass);
            }
        }
        
        ReflectedMethod method = getMethod(methodName, clazz, returnType, parameterCount, args);
        if (method == null) return null;
        return method.invoke(instance, arguments);
    }
    
    // Cache for field names to reduce reflection calls during UI Crawling
    public static boolean hasVariableOfName(String name, Object instance) throws Throwable {
        Class<?> clazz = instance.getClass();
        return fieldNameCache.computeIfAbsent(clazz, k -> {
            // Class has not been cached yet, save all method names to the cache
            HashSet<String> set = new HashSet<>();
            Field[] instancesOfFields = instance.getClass().getDeclaredFields();
            for (Field field : instancesOfFields) {
                try {
                    set.add((String) getFieldNameHandle.invoke(field));
                } catch (Throwable e) {
                    // Handle exception
                }
            }
            return set;
        }).contains(name);
    }
    
    // Cache for field types to reduce reflection calls during UI Crawling
    public static boolean hasVariableOfType(Class<?> type, Object instance) throws Throwable {
        Class<?> clazz = instance.getClass();
        return fieldTypesCache.computeIfAbsent(clazz, k -> {
            // Class has not been cached yet, save all method names to the cache
            HashSet<Class<?>> set = new HashSet<>();
            Field[] instancesOfFields = instance.getClass().getDeclaredFields();
            for (Field field : instancesOfFields) {
                try {
                    set.add((Class<?>) getFieldTypeHandle.invoke(field));
                } catch (Throwable e) {
                    // Handle exception
                }
            }
            return set;
        }).contains(type);
    }
    
    // Cache for method names to reduce reflection calls during UI Crawling
    public static boolean hasMethodOfName(String name, Object instance) throws Throwable {
        Class<?> clazz = instance.getClass();
        return methodNameCache.computeIfAbsent(clazz, k -> {
            // Class has not been cached yet, save all method names to the cache
            HashSet<String> set = new HashSet<>();
            Method[] instancesOfMethods = instance.getClass().getDeclaredMethods();
            for (Method method : instancesOfMethods) {
                try {
                    set.add((String) getMethodNameHandle.invoke(method));
                } catch (Throwable e) {
                    // Handle exception
                }
            }
            return set;
        }).contains(name);
    }
    
    public static ReflectedField getField(String fieldName, Class<?> fieldClazz) throws Throwable {
        return getField(fieldName, fieldClazz, null, null, null);
    }
    
    public static ReflectedField getField(String fieldName, Class<?> fieldClazz, Class<?> fieldType) throws Throwable {
        return getField(fieldName, fieldClazz, fieldType, null, null);
    }
    
    public static ReflectedField getField(String fieldName, Class<?> fieldClazz, Class<?> fieldType, Integer recursionLimit, Class<?> superclazz) throws Throwable {
        Class<?> clazz;
        if (superclazz != null) clazz = superclazz;
        else {
            clazz = fieldClazz;
        }

        boolean withSuper = false;
        if (recursionLimit != null) withSuper = true;
        
        ReflectedFieldKey key = new ReflectedFieldKey(clazz, fieldName, fieldType, withSuper);
        return fieldsCache.computeIfAbsent(key, k -> {
            try {
                Object targetField = null;
                
                // Combine public and declared fields
                List<Field> allFields = new ArrayList<>();
                allFields.addAll(Arrays.asList(clazz.getFields()));
                allFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
                
                for (Field field : allFields) {
                    if (fieldName != null) {
                        String name = (String) getFieldNameHandle.invoke(field);
                        if (!fieldName.equals(name)) continue;
                    }
                    
                    if (fieldType != null) {
                        Class<?> type = (Class<?>) getFieldTypeHandle.invoke(field);
                        if (!fieldType.equals(type)) continue;
                    }
                    
                    targetField = field;
                    break;
                }
                
                if (targetField == null && recursionLimit != null && recursionLimit > 0) {
                    Class<?> superc = clazz.getSuperclass();
                    if (superc != null) {
                        return getField(fieldName, clazz, fieldType, recursionLimit - 1, superc);
                    }
                }
                
                // Should not crash just because it could not find it.
                if (targetField == null) {
                    return null;
                }
                
                setFieldAccessibleHandle.invoke(targetField, true);
                return new ReflectedField(targetField);
            } catch (Throwable e) {
                return null;
            }
        });
    }
    
    public static ReflectedMethod getMethod(String methodName, Class<?> clazz) throws Throwable {
        return getMethod(methodName, clazz, null, null, null);
    }
    
    public static ReflectedMethod getMethod(String methodName, Class<?> clazz, Class<?> returnType, Integer parameterCount, List<Class<?>> parameters) throws Throwable {
        ReflectedMethodKey key = new ReflectedMethodKey(clazz, methodName, parameterCount, returnType, parameters);
        return methodsCache.computeIfAbsent(key, k -> {
            try {
                Object targetMethod = null;
                
                // Combine public and declared methods
                Set<Method> allMethods = new HashSet<>();
                allMethods.addAll(Arrays.asList(clazz.getMethods()));
                allMethods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
                
                for (Method method : allMethods) {
                    if (methodName != null) {
                        String name = (String) getMethodNameHandle.invoke(method);
                        if (!methodName.equals(name)) continue;
                    }
                    
                    Class<?>[] methodParams = (Class<?>[]) getMethodParametersHandle.invoke(method);
                    
                    if ((parameters != null && !parameters.isEmpty()) || parameterCount != null) {
                        // Skip if parameters do not match
                        if (parameters != null && !parameters.isEmpty()) {
                            boolean paramsMatch = true;
                            for (Class<?> param : methodParams) {
                                if (!parameters.contains(param)) {
                                    paramsMatch = false;
                                    break;
                                }
                            }
                            if (!paramsMatch) continue;
                        }
                        
                        // Skip if parameters count does not match
                        if (parameterCount != null) {
                            if (methodParams.length != parameterCount) continue;
                        }
                    }
                    
                    if (returnType != null) {
                        Class<?> type = (Class<?>) getMethodReturnTypeHandle.invoke(method);
                        if (!returnType.equals(type)) continue;
                    }
                    
                    targetMethod = method;
                    break;
                }
                
                // Should not crash just because it could not find it.
                if (targetMethod == null) {
                    return null;
                }
                
                return new ReflectedMethod(targetMethod);
            } catch (Throwable e) {
                return null;
            }
        });
    }
    
    // Useful for some classes with just one field
    public static Object getFirstDeclaredField(Object instanceToGetFrom) throws Throwable {
        Field[] fields = instanceToGetFrom.getClass().getDeclaredFields();
        if (fields.length == 0) return null;
        
        Field field = fields[0];
        setFieldAccessibleHandle.invoke(field, true);
        return getFieldHandle.invoke(field, instanceToGetFrom);
    }
}