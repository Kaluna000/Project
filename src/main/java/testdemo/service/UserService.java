package testdemo.service;

import springx.*;

@Scope("prototype")
@Component("userService")
public class UserService implements BeanNameAware, InitializingBean{
    @Autowired
    private OrderService orderService;

    private String beanName;

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("初始化");
    }


    public void test() {
        System.out.println(orderService);
        System.out.println(beanName);
    }
}
