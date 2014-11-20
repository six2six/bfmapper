package br.com.bfmapper.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang.ClassUtils;
import org.springframework.beans.BeanUtils;

public class ReflectionUtils {

    public static final String CGLIB_CLASS_SEPARATOR = "$$";
    
    private static Map<Class<?>, Class<?>> primitiveToWrapperMap = new HashMap<Class<?>, Class<?>>(); 
    private static Map<Class<?>, Class<?>> wrapperToprimitiveMap = new HashMap<Class<?>, Class<?>>();

    static {
        primitiveToWrapperMap.put(byte.class, Byte.class);
        primitiveToWrapperMap.put(char.class, Character.class);
        primitiveToWrapperMap.put(short.class, Short.class);
        primitiveToWrapperMap.put(long.class, Long.class);
        primitiveToWrapperMap.put(int.class, Integer.class);
        primitiveToWrapperMap.put(float.class, Float.class);
        primitiveToWrapperMap.put(double.class, Double.class);
        primitiveToWrapperMap.put(boolean.class, Boolean.class);
        
        wrapperToprimitiveMap.put(Byte.class, byte.class);
        wrapperToprimitiveMap.put(Character.class, char.class);
        wrapperToprimitiveMap.put(Short.class, short.class);
        wrapperToprimitiveMap.put(Long.class, long.class);
        wrapperToprimitiveMap.put(Integer.class, int.class);
        wrapperToprimitiveMap.put(Float.class, float.class);
        wrapperToprimitiveMap.put(Double.class, double.class);
        wrapperToprimitiveMap.put(Boolean.class, boolean.class);
    }
    
    
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object source){
        try {
            return source == null ? null : (T) source;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert to type", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T invokeGetter(Object bean, String attribute) {
        return (T) ReflectionUtils.invokeGetter(bean, attribute, true);
    }
    
    protected static PropertyUtilsBean getPropertyUtilsBean() {
        return BeanUtilsBean.getInstance().getPropertyUtils();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T invokeGetter(Object bean, String attribute, boolean fail) {
        try {
            return (T) getPropertyUtilsBean().getProperty(bean, attribute);
        }catch (Exception e) {
            if (fail) {
                throw new IllegalArgumentException("Error invoking get method for " + attribute, e);   
            } else {
                return null;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object bean, String fieldName, boolean fail){
        try {
            Field field = bean.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(bean);
        } catch (NoSuchFieldException ex){
            if(fail) {
                throw new IllegalArgumentException("Field " + fieldName + " doesn't exists", ex);
            } else {
                return null;
            }
        }catch (Exception e) {
            throw new IllegalArgumentException("Error getting field " + fieldName, e);
        }           
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object bean, String fieldName){
        return (T) ReflectionUtils.getFieldValue(bean, fieldName, true);
    }
    
    
    public static Method invokeAutoBoxingMethod(Object bean, String methodName, Class<?> type) {
        Method method = null;
        try {
            method = bean.getClass().getMethod(methodName, type);
        } catch (SecurityException e) {
            throw new IllegalArgumentException("error calling method " + methodName, e);
        } catch (NoSuchMethodException e) {
            try {
                if (wrapperToprimitiveMap.get(type) != null) {
                    method = bean.getClass().getMethod(methodName,  wrapperToprimitiveMap.get(type));
                } else if (primitiveToWrapperMap.get(type) != null) {
                    method = bean.getClass().getMethod(methodName,  primitiveToWrapperMap.get(type));
                }
            } catch (Exception e2) {
                throw new IllegalArgumentException("error calling method " + methodName, e);
            }
            
        }
        
        return method;
    }
    
    public static Object invokeRecursiveGetter(Object bean, String objectsPath) {
        Object lastValue = null;
        Object lastBean = bean;
        for (String propertyItem : objectsPath.split("\\.")) {
            lastValue = ReflectionUtils.invokeGetter(lastBean, propertyItem);
            lastBean = lastValue;
            if (lastValue == null) {
                break;
            }
        }
        return lastValue;
    }
    
    public static <T> void invokeSetter(Object bean, String attribute, Object value, boolean fail){
        try {
            getPropertyUtilsBean().setProperty(bean, attribute, value);
        } catch (Exception ex){
            if(fail) {
                throw new IllegalArgumentException("Method for set attribute " + attribute, ex);
            }
        }   
    }
    
    public static <T> void invokeSetter(Object bean, String attribute, Object value){
        ReflectionUtils.invokeSetter(bean, attribute, value, true);
    }       
    
    public static Object prepareInvokeRecursiveSetter(Object bean, String attribute, Object value) {
        Object targetBean = bean;
        Object lastBean = null;
        
        int lastAttributeIdx = attribute.lastIndexOf(".");
        
        String path  = null;
        if (lastAttributeIdx > 0) {
            path = StringUtil.defaultIfEmpty(attribute.substring(0, lastAttributeIdx), null);
        }
        
        if (path != null) {
            for (String propertyItem : path.split("\\.")) {
                lastBean = targetBean;
                targetBean = ReflectionUtils.invokeGetter(targetBean, propertyItem);
                if(targetBean == null) {
                    try {
                        targetBean = newInstance(BeanUtils.getPropertyDescriptor(lastBean.getClass(), propertyItem).getPropertyType());
                        ReflectionUtils.invokeSetter(lastBean, propertyItem, targetBean, true);                     
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Method " + propertyItem + " doesn't exists", e);
                    }
                }
            }
        }
        
        return targetBean;
        
    }
    
    public static void invokeRecursiveSetter(Object bean, String attribute, Object value) {
        ReflectionUtils.invokeSetter(prepareInvokeRecursiveSetter(bean, attribute, value), attribute.substring(attribute.lastIndexOf(".") + 1), value, true);
    }
    
    public static Class<?> invokeRecursiveType(Object bean, String attribute) {
    	return invokeRecursiveType(getTargetClass(bean.getClass()), attribute);
    }

    public static Class<?> invokeRecursiveType(Class<?> clazz, String attribute) {
        for (String propertyItem : attribute.split("\\.")) {
            try {
            	clazz = BeanUtils.getPropertyDescriptor(clazz, propertyItem).getPropertyType();
            } catch (Exception e) {
                throw new IllegalArgumentException("Field " + propertyItem + " doesn't exists", e);
            }
        }
        return clazz;
    }

    
    public static Field invokeRecursiveField(Object bean, String attribute) {
        Field field = null;
        Class<?> superClass = null;
        Class<?> targetBeanClass = getTargetClass(bean.getClass());
        
        for (String propertyItem : attribute.split("\\.")) {
            do {
                superClass = targetBeanClass.getSuperclass();
                try {
                    field = targetBeanClass.getDeclaredField(propertyItem);
                    targetBeanClass = field.getType();
                } catch (NoSuchFieldException e) {
                    targetBeanClass = superClass;
                } 
            } while (superClass != null && !superClass.equals(Object.class));
        }
        
        if (field == null) {
            throw new IllegalArgumentException("Field " + attribute + " doesn't exists");
        }
        
        return field;
    }

    public static Class<?>[] getArgumentTypes(Object[] argsValues) {
        Class<?>[] types = null;
        if (argsValues != null) {
            types = new Class[argsValues.length];
            for (int i = 0; i < types.length; i++) {
                types[i] = argsValues[i].getClass();
            }
        }
        return types;
    }
    
    public static Method getProxyMethod(Method source, Object proxy) {
        Method proxyMethod = null;
        try {
            proxyMethod = proxy.getClass().getMethod(source.getName(), source.getParameterTypes());
        } catch (Exception e) { /* ignoring exception */ } 

        return proxyMethod;
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> clazz) {
        Object instance = null;
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return (T) instance;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T newInnerClassInstance(Class<?> clazz, Object owner) {
    	Object instance = null;
    	try {
    		Constructor<?> constructor = clazz.getDeclaredConstructor(owner.getClass());
    		constructor.setAccessible(true);
    		instance = constructor.newInstance(owner);
    	} catch (Exception e) {
    		throw new IllegalArgumentException(e);
    	}

    	return (T) instance;
    }
    
    public static Class<?> getGenericClass(Class<?> clazz, int argument) {
        return (Class<?>) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[argument];
    }

    public static Class<?> getGenericClassFromField(Field field, int argument) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[argument];
    }
    
    public static Class<?> getGenericClassFromRecursiveField(Object bean, String attribute, int argument) {
        return getGenericClassFromField(invokeRecursiveField(bean, attribute), argument);
    }
    
    public static boolean isSimpleType(Class<?> clazz) {
        return primitiveToWrapperMap.containsKey(clazz) || primitiveToWrapperMap.containsValue(clazz) || 
               clazz.equals(String.class) || clazz.equals(Date.class) || clazz.equals(Calendar.class) || 
               clazz.isEnum() || ReflectionUtils.isAssignable(clazz, Number.class);  
    }
    
    public static boolean isPrimitiveWrapperAssignable(Class<?> source, Class<?> target) {
        boolean result = source.equals(target);
        if (!result) {
            result = (primitiveToWrapperMap.get(source) != null && primitiveToWrapperMap.get(source).equals(target)) || (wrapperToprimitiveMap.get(source) != null && wrapperToprimitiveMap.get(source).equals(target));  
        }
        return result;
    }
    
    public static boolean isCglibProxy(Class<?> clazz) {
        return (clazz != null && clazz.getName().indexOf(CGLIB_CLASS_SEPARATOR) != -1);
    }
    
    public static Class<?> getTargetClass(Class<?> clazz) {
        if (isCglibProxy(clazz) || Proxy.isProxyClass(clazz)) {
            clazz = clazz.getSuperclass();
        }
        return clazz;
    }
    
    public static boolean isInnerClass(Class<?> clazz) {
    	return clazz.getEnclosingClass() != null && !Modifier.isStatic(clazz.getModifiers());
    }
    
    public static boolean isAssignable(Class<?> clazz1, Class<?> clazz2) {
    	try {
			ClassLoader clazz1ClassLoader = clazz1.getClassLoader();
			if(clazz1ClassLoader != null) {
				Class<?> classToCompare = clazz1ClassLoader.loadClass(clazz2.getName());
				return ClassUtils.isAssignable(clazz1, classToCompare);
			} else {
				return ClassUtils.isAssignable(clazz1, clazz2);
			}
		} catch (ClassNotFoundException e) {
			return ClassUtils.isAssignable(clazz1, clazz2);
		}
    }

}
