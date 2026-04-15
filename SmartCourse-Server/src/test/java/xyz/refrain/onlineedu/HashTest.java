package xyz.refrain.onlineedu;

import cn.hutool.crypto.digest.MD5;

public class HashTest {
    public static void main(String[] args) {
        String salt = "1";
        MD5 md5 = new MD5(salt.getBytes());
        String hash123456 = md5.digestHex("123456");
        String hash111111 = md5.digestHex("111111");
        String hashadmin = md5.digestHex("admin");
        
        System.out.println("HASH_123456=" + hash123456);
        System.out.println("HASH_111111=" + hash111111);
        System.out.println("HASH_admin=" + hashadmin);
    }
}
