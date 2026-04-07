package xyz.refrain.onlineedu; 
import xyz.refrain.onlineedu.utils.SessionUtils; 
public class PasswordTest { 
    public static void main(String[] args) { 
        System.out.println("PWD 111111: " + SessionUtils.encodePassword("111111")); 
        System.out.println("PWD 123456: " + SessionUtils.encodePassword("123456")); 
        System.out.println("PWD admin: " + SessionUtils.encodePassword("admin")); 
        System.out.println("PWD tm050711: " + SessionUtils.encodePassword("tm050711")); 
    } 
}