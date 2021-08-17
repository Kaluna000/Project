package testdemo;

import springx.SpringApplicationContext;
import testdemo.service.UserService;
import testdemo.service.UserServiceInterface;

public class Test {
    public static void main(String[] args) {
        SpringApplicationContext springApplicationContext = new SpringApplicationContext(AppConfig.class);
//        UserServiceInterface userService = (UserServiceInterface) springApplicationContext.getBean("userService");
        UserService userService = (UserService) springApplicationContext.getBean("userService");
        userService.test();
    }
}
