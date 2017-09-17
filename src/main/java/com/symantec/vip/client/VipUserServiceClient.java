package com.symantec.vip.client;

import com.symantec.vipuc.ws.v_1_x.GetServerTimeRequestType;
import com.symantec.vipuc.ws.v_1_x.GetServerTimeResponseType;
import com.symantec.vipuc.ws.v_1_x.QueryServicePort;

/**
 * Created by shantanu_gattani
 */
public class VipUserServiceClient {
    //Set certificate path here
    private static final String CERT = "certs/vip_cert.p12";
    private static final String PASSWORD = "Password1";

    private VipUserServiceCallTemplate callTemplate = new VipUserServiceCallTemplate(CERT, PASSWORD, 2);

    public GetServerTimeResponseType getServerTime(VipUserServiceCallTemplate template, String requestId) {
        GetServerTimeRequestType req = new GetServerTimeRequestType();
        req.setRequestId(requestId);

        GetServerTimeResponseType resp = template.callUserService(UCPortType.QUERY, req, new VipUserServiceCallTemplate.VipUserServiceCallback<GetServerTimeRequestType, GetServerTimeResponseType>() {
            @Override
            public GetServerTimeResponseType execute(PortT portT, GetServerTimeRequestType request) {
                QueryServicePort queryServicePort = (QueryServicePort) portT.getPort();
                return queryServicePort.getServerTime(request);
            }
        });

        return resp;
    }

    public VipUserServiceCallTemplate getCallTemplate() {
        return callTemplate;
    }

}
