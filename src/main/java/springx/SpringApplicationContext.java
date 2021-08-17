package springx;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SpringApplicationContext {
    private Class configClass;

    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>();  //单例池，存储单例对象
    private ConcurrentHashMap<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();  //存储定义类对象
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();  //存储BeanPostProcessor对象

    public SpringApplicationContext(Class configClass) {
        this.configClass = configClass;

        scan(configClass);  //ComponentScan注解->扫描路径->BeanDefinition ->BeanDefinitionMap
        //为beanDefinitionMap中的所有单例bean创建对象，并放入单例池中
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanName,beanDefinition);
                singletonObjects.put(beanName,bean);
            }
        }



    }
    public Object createBean(String beanName,BeanDefinition beanDefinition){
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            //依赖注入
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)){
                    Object bean = getBean(field.getName());
                    field.setAccessible(true);
                    field.set(instance,bean);
                }
            }
            //Aware回调
            if (instance instanceof BeanNameAware){
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            //调用BeanPostProcessor中的初始化前方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);   //返回可能修改后的实例
            }

            //初始化
            if (instance instanceof InitializingBean){
                try {
                    ((InitializingBean) instance).afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //调用BeanPostProcessor的初始化后方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                 instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }

            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void scan(Class configClass) {
        //读取配置类上定义的Spring相关注解
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        //获取扫描路径
        String path = componentScanAnnotation.value();
        path = path.replace(".","/");
        System.out.println(path);
        //扫描路径包下的类，判断是否加上了Component注解
        ClassLoader classLoader = SpringApplicationContext.class.getClassLoader();  //获得APP类加载器
        URL resource = classLoader.getResource(path);
        File file = new File(resource.getFile());
        if (file.isDirectory()){
            File[] files = file.listFiles();
            for (File f:files){
                String fileName = f.getPath();
                System.out.println(fileName);
                if (fileName.endsWith(".class")){
                    String className = fileName.substring(fileName.indexOf("classes"),fileName.indexOf(".class"));
                    className = className.substring(8);
                    className = className.replace("\\", ".");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (clazz.isAnnotationPresent(Component.class)){
                            //表示当前类是一个Bean，先判断是不是BeanPostProcessor
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)) { //它是不是实现了该接口
                                BeanPostProcessor instance = (BeanPostProcessor)clazz.getDeclaredConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }
                            //如果不是BeanPostProcessor，接着判断是单例bean还是原型bean
                            //定义类 -> BeanDefinition：标识该Bean是单例bean还是原型bean
                            //获取BeanName
                            Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
                            String beanName = componentAnnotation.value();
                            BeanDefinition beanDefinition = new BeanDefinition();   //创建定义类对象
                            beanDefinition.setClazz(clazz);
                            if (clazz.isAnnotationPresent(Scope.class)){    //如果不是单例
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName,beanDefinition);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Object getBean(String beanName){
        //先到定义类Map中查看bean是单例还是原型，如果是单例就到单例池中取，是原型就创建新对象
        if (beanDefinitionMap.containsKey(beanName)){
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")){
                Object o = singletonObjects.get(beanName);
                return o;
            }else {
                Object bean = createBean(beanName,beanDefinition);
                return bean;
            }
        }else {
            throw new NullPointerException("不存在对应的Bean");
        }
    }


}
