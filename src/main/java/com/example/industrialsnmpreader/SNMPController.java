package com.example.industrialsnmpreader;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SNMPController {

    public static class VendorOids {
        public final String hostnameOid;
        public final String tempOid;
        public final String uptimeOid;
        public final String cpuOid;

        public VendorOids(String hostname, String temp, String uptime, String cpu) {
            this.hostnameOid = hostname;
            this.tempOid = temp;
            this.uptimeOid = uptime;
            this.cpuOid = cpu;
        }
    }

    public static VendorOids getOidsForVendor(String vendor) {
        if (vendor == null) return null;
        String commonHostname = "1.3.6.1.2.1.1.5.0";
        String commonUptime = "1.3.6.1.2.1.1.3.0";

        return switch (vendor.toLowerCase()) {
            case "siemens" -> new VendorOids(
                    commonHostname,
                    "1.3.6.1.4.1.261.2.1.1.1.1.0",
                    commonUptime,
                    "1.3.6.1.4.1.261.2.1.1.3.1.0"
            );
            case "mikrotik" -> new VendorOids(
                    commonHostname,
                    "1.3.6.1.4.1.14988.1.1.3.11.0",
                    commonUptime,
                    "1.3.6.1.2.1.25.3.3.1.2.2"
            );
            default -> null;
        };
    }

    public static String getSnmpValue(String ipAddress, String community, String oidString) {
        if (oidString == null || oidString.isEmpty()) return "Brak OID";
        
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

                int syntax = var.getSyntax();
                if (syntax == SMIConstants.EXCEPTION_NO_SUCH_OBJECT ||
                        syntax == SMIConstants.EXCEPTION_NO_SUCH_INSTANCE) {
                    return "Błędny OID";
                }
                
                if (var instanceof TimeTicks) {
                    return ((TimeTicks) var).toString();
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