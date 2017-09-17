package com.symantec.vip.client;

import java.net.URL;

import com.symantec.vip.helper.SslSocketFactoryUtil;
import com.symantec.vip.helper.Utils;
import com.symantec.vipuc.ws.v_1_x.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.xml.ws.BindingProvider;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.sun.xml.ws.client.BindingProviderProperties;

/**
 * Created by shantanu_gattani
 */
public class VipUserServiceCallTemplate {
    private String vipUserServiceAuthWsdlLocation ="wsdl/vipuserservices-auth-1.8.wsdl";
    private String vipUserServiceAuthServiceEndpoint ="https://userservices-auth.vip.symantec.com/vipuserservices/AuthenticationService_1_8";

    private String vipUserServiceQueryWsdlLocation ="wsdl/vipuserservices-query-1.8.wsdl";
    private String vipUserServiceQueryServiceEndpoint ="https://userservices-auth.vip.symantec.com/vipuserservices/QueryService_1_8";
    
    private final String clientCertFile;
    private final String clientCertPassword;

    private static final long BORROW_TIMEOUT_SECONDS = 5;

    private final BlockingDeque<AuthenticationServicePort> authenticationServicePorts = new LinkedBlockingDeque<>();
    private final BlockingDeque<QueryServicePort> queryServicePorts = new LinkedBlockingDeque<>();

    public VipUserServiceCallTemplate(String vipUserServiceAuthWsdlLocation, String vipUserServiceAuthServiceEndpoint, String vipUserServiceQueryWsdlLocation, String vipUserServiceQueryServiceEndpoint, String clientCertFile, String clientCertPassword, int maxCapacity) {
        this.vipUserServiceAuthWsdlLocation = vipUserServiceAuthWsdlLocation;
        this.vipUserServiceAuthServiceEndpoint = vipUserServiceAuthServiceEndpoint;
        this.vipUserServiceQueryWsdlLocation = vipUserServiceQueryWsdlLocation;
        this.vipUserServiceQueryServiceEndpoint = vipUserServiceQueryServiceEndpoint;
        this.clientCertFile = clientCertFile;
        this.clientCertPassword = clientCertPassword;

        System.out.println("*****************" + this.getClass().getClassLoader().getResource(clientCertFile).getPath());
        System.out.println("*****************" + this.getClass().getClassLoader().getResource(vipUserServiceAuthWsdlLocation).getPath());
        System.out.println("*****************" + this.getClass().getClassLoader().getResource(vipUserServiceQueryWsdlLocation).getPath());
        System.out.println("*****************" + vipUserServiceAuthServiceEndpoint);
        System.out.println("*****************" + vipUserServiceQueryServiceEndpoint);

        for(int i = 0; i < maxCapacity; i++) {
            PortT<AuthenticationServicePort> port = createServicePort(UCPortType.AUTH);
            authenticationServicePorts.offer(port.getPort());
        }

        for(int i = 0; i < maxCapacity; i++) {
            PortT<QueryServicePort> port = createServicePort(UCPortType.QUERY);
            queryServicePorts.offer(port.getPort());
        }

    }

    public VipUserServiceCallTemplate(String clientCertFile, String clientCertPassword, final int maxCapacity) {
        this.clientCertFile = clientCertFile;
        this.clientCertPassword = clientCertPassword;

        for(int i = 0; i < maxCapacity; i++) {
            PortT<AuthenticationServicePort> port = createServicePort(UCPortType.AUTH);
            authenticationServicePorts.offer(port.getPort());
        }

        for(int i = 0; i < maxCapacity; i++) {
            PortT<QueryServicePort> port = createServicePort(UCPortType.QUERY);
            queryServicePorts.offer(port.getPort());
        }
    }

    public <RequestT, ResponseT> ResponseT callUserService(UCPortType portType, RequestT request, VipUserServiceCallback<RequestT, ResponseT> callback) {
        long startTime, endTime;
        PortT port = borrowPort(portType);
        startTime = System.currentTimeMillis();
        ResponseT resp = null;
        try {
            resp = callback.execute(port, request);
        } catch (Exception ex) {
            System.out.println("Exception thrown while attempting to invoke UC API. Discarding borrowed proxy and creating a new one. " + ex);
            // Mark the vipProxy as null, before attempting to create a new instance, as we check for null
            // in the finally {} block.
            port = null;
            port = createServicePort(portType);
        } finally {
            endTime = System.currentTimeMillis();
            if(port != null) {
                returnPort(port);
            }
        }

        if (resp != null) {
            BaseResponseType baseResponseType = (BaseResponseType)resp;
            String responseStatusStr = Utils.byteArrayToHexString(baseResponseType.getStatus());
            String responseStatusMessage = baseResponseType.getStatusMessage();
            System.out.println("VIP User Services status code: " + responseStatusStr + " message: " + responseStatusMessage + ". Time taken: " + (endTime-startTime) +  "ms." );
            return resp;
        } else {
            System.out.println("Response is null while attempting to invoke VIP Services API. Total time taken: " + (endTime - startTime) + "ms");
            throw new RuntimeException("Invoking UserServicesAPI failed.");
        }
    }

    private PortT createServicePort(UCPortType portType) {
        BindingProvider bp;
        URL url;
        String endpoint;

        switch(portType){
            case AUTH:
                url = this.getClass().getClassLoader().getResource(vipUserServiceAuthWsdlLocation);
                endpoint = vipUserServiceAuthServiceEndpoint;
                System.out.println("Creating proxy for URL: " + url + " and endpoint: " + endpoint);
                AuthenticationService authenticationService = new AuthenticationService(url);
                AuthenticationServicePort authPort = authenticationService.getAuthenticationServicePort();
                bp = (BindingProvider)authPort;
                bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
                bp.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, Integer.valueOf(5000));
                bp.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, Integer.valueOf(20000)); //20 seconds
                bp.getRequestContext().put(BindingProviderProperties.SSL_SOCKET_FACTORY,
                        SslSocketFactoryUtil.getSSLSocketFactory(clientCertFile, clientCertPassword));
                bp.getRequestContext().put(BindingProviderProperties.HOSTNAME_VERIFIER, new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        System.out.println("HostnameVerifier called for host name: " + hostname +  " SSLSession: " + session);
                        return true;
                    }
                });
                PortT<AuthenticationServicePort> authPortT = new PortT<>();
                authPortT.setType(UCPortType.AUTH);
                authPortT.setPort(authPort);
                return authPortT;

            case QUERY:
            default:
                url = this.getClass().getClassLoader().getResource(vipUserServiceQueryWsdlLocation);
                endpoint = vipUserServiceQueryServiceEndpoint;
                System.out.println("Creating proxy for URL: " + url + " and endpoint: " + endpoint);
                QueryService queryService = new QueryService(url);
                QueryServicePort queryServicePort = queryService.getQueryServicePort();
                bp = (BindingProvider)queryServicePort;
                bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
                bp.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, Integer.valueOf(5000));
                bp.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, Integer.valueOf(5000)); 
                bp.getRequestContext().put(BindingProviderProperties.SSL_SOCKET_FACTORY,
                        SslSocketFactoryUtil.getSSLSocketFactory(clientCertFile, clientCertPassword));
                bp.getRequestContext().put(BindingProviderProperties.HOSTNAME_VERIFIER, new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        System.out.println(Thread.currentThread().getName() + " HostnameVerifier called for host name: " + hostname +  " SSLSession: " + session);
                        return true;
                    }
                });
                
                PortT<QueryServicePort> queryPortT = new PortT<>();
                queryPortT.setType(UCPortType.QUERY);
                queryPortT.setPort(queryServicePort);
                return queryPortT;
        }
        
    }    
    
    public PortT borrowPort(UCPortType portType)	{
        PortT retVal = new PortT<>();
        switch (portType){
            case AUTH:
                try {
                    AuthenticationServicePort port = authenticationServicePorts.pollLast(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    retVal.setPort(port);
                } catch (InterruptedException ie){
                    System.out.println("*** Interrupted exception thrown while borrowing the authentication port. " + ie);    
                }

                // This is the worst case scenario, so create a new instance and return.
                if(retVal == null) {
                    System.out.println("*** WARNING *** Borrowing proxy from AUTH pool returns null. Will create a proxy and return.");
                    retVal = createServicePort(UCPortType.AUTH);
                }
                break;
            
            default:
                try {
                    QueryServicePort port = queryServicePorts.pollLast(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    retVal.setPort(port);
                } catch (InterruptedException ie){
                    System.out.println("*** Interrupted exception thrown while borrowing the query port. " + ie);
                }

                // This is the worst case scenario, so create a new instance and return.
                if(retVal == null) {
                    System.out.println("*** WARNING *** Borrowing proxy from QUERY pool returns null. Will create a proxy and return.");
                    retVal = createServicePort(UCPortType.QUERY);
                }
                break;
        }

        return retVal;
    }
    
    protected void returnPort(PortT port) {
        if(port == null) {
            return;
        }
        
        if(port.getType() == UCPortType.AUTH){
            if(!authenticationServicePorts.offerLast((AuthenticationServicePort)port.getPort())) {
                System.out.println("*** WARNING *** Authentication port pool is already full. Discarding the proxy being returned.");
            }            
        } else {
            if(!queryServicePorts.offerLast((QueryServicePort)port.getPort())) {
                System.out.println("*** WARNING *** Query port pool is already full. Discarding the proxy being returned.");
            }
        }
    }
    
    public static interface VipUserServiceCallback<RequestT, ResponseT> {
        public ResponseT execute(PortT port, RequestT request);
    }


}
