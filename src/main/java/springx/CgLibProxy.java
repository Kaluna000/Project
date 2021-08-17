package springx;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

@Component
public  class CgLibProxy implements Callback {
    // Generates dynamic subclasses to enable method interception
    private Enhancer enhancer = new Enhancer();
    private Object bean;
    private MethodInterceptor methodInterceptor;

    public CgLibProxy(Object bean, MethodInterceptor methodInterceptor) {
        this.bean = bean;
        this.methodInterceptor = methodInterceptor;
    }


    public  <T>T getProyInfo(){

        return (T)enhancer.create(bean.getClass(),methodInterceptor);
    }





}

