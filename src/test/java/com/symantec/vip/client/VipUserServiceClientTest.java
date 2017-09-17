package com.symantec.vip.client;

import org.junit.Test;
import com.symantec.vip.client.VipUserServiceClient;
import java.security.Security;

public class VipUserServiceClientTest {

    @Test
    public void myGetServerTimeTest(){
        System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
        Security.setProperty("networkaddress.cache.ttl", "0");
        Security.setProperty("networkaddress.cache.negative.ttl", "0");

        VipUserServiceClient client = new VipUserServiceClient();

        System.out.println("Request using JH1");
        System.out.println("ServerTime =" + client.getServerTime(client.getCallTemplate(), "templateReq1").getTimestamp());
        System.out.println("ServerTime =" + client.getServerTime(client.getCallTemplate(), "templateReq2").getTimestamp());
        System.out.println("ServerTime =" + client.getServerTime(client.getCallTemplate(), "templateReq3").getTimestamp());
    }
}