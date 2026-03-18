package com.example.industrialsnmpreader;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SNMPController {

    public static String getSnmpValue(String ipAddress, String community, String oidString) {
        Snmp snmp = null;
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
            snmp = new Snmp(transport);
            transport.listen();

            ResponseEvent<Address> response = snmp.get(pdu, target);

            if (response != null && response.getResponse() != null) {
                Variable var = response.getResponse().get(0).getVariable();

                // POPRAWKA: Użycie SMIConstants zamiast SnmpConstants
                int syntax = var.getSyntax();
                if (syntax == SMIConstants.EXCEPTION_NO_SUCH_OBJECT ||
                        syntax == SMIConstants.EXCEPTION_NO_SUCH_INSTANCE) {
                    return "Błędny OID";
                }
                return var.toString();
            }
            return "Err: Timeout";
        } catch (Exception e) {
            return "Err: " + e.getMessage();
        } finally {
            if (snmp != null) {
                try { snmp.close(); } catch (Exception e) { }
            }
        }
    }
}