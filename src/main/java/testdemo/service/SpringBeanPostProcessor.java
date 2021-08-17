package testdemo.service;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import springx.BeanPostProcessor;
import springx.CgLibProxy;
import springx.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (beanName.equals("userService")){
            System.out.println("初始化前");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if ("userService".equals(beanName)){
            System.out.println("初始化后");
            ArrayList<Object> interfacesList = new ArrayList<>();
            for (Class<?> anInterface : bean.getClass().getInterfaces()) {
                if (!"springx.BeanNameAware".equals(anInterface.getName()) && !"springx.InitializingBean".equals(anInterface.getName())){
                    interfacesList.add(anInterface);
                }
            }
            if (!interfacesList.isEmpty()){
                System.out.println("使用JDK进行动态代理");
                if (bean instanceof UserService){
                    ((UserService) bean).test();
                }
                Object proxyInstance = Proxy.newProxyInstance(SpringBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("JDK执行代理逻辑1");
                        Object res = method.invoke(bean, args);//执行被代理对象方法
                        System.out.println("JDK执行代理逻辑2");
                        return res;
                    }
                });
                return proxyInstance;
            }else {
                System.out.println("使用cglib进行动态代理");
                if (bean instanceof UserService){
                     ((UserService) bean).test();
                }
                //生成代理对象，重写代理逻辑
                CgLibProxy cgLibProxy = new CgLibProxy(bean, new MethodInterceptor() {
                    @Override
                    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                        System.out.println("-------调用方法之前---------------");
                        // 调用父类中方法
                        Object res = method.invoke(bean, args);//执行被代理对象方法
                        System.out.println("-------调用方法之后---------------");
                        return res;
                    }
                });
                return cgLibProxy.getProyInfo();
            }
        }
        return bean;
    }
}
