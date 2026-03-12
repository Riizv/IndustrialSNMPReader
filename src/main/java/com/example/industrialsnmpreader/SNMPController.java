package com.example.industrialsnmpreader;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SNMPController {

    public static String getSnmpValue(String ipAddress, String community, String oidString) {
        try {
            Address targetAddress = GenericAddress.parse("udp:" + ipAddress + "/161");
            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setCommunity(new OctetString(community));
            target.setAddress(targetAddress);
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oidString)));
            pdu.setType(PDU.GET);

            TransportMapping<? extends Address> transport = new DefaultUdpTransportMapping();
            transport.listen();
            Snmp snmp = new Snmp(transport);

            ResponseEvent<Address> response = snmp.get(pdu, target);
            String result = "Err";

            if (response != null && response.getResponse() != null) {
                // Pobieramy samą wartość (bez OID)
                result = response.getResponse().get(0).getVariable().toString();
            }

            snmp.close();
            return result;
        } catch (Exception e) {
            return "Err: " + e.getMessage();
        }
    }
}