package com.example.industrialsnmpreader;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
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

    public static String getSnmpValue(Device device, String oidString) {
        if (oidString == null || oidString.isEmpty()) return "Brak OID";
        
        Snmp snmp = null;
        try {
            TransportMapping<? extends Address> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);

            Target<Address> target;
            PDU pdu;

            if (device.getSnmpVersion() == 3) {
                // Konfiguracja SNMP v3
                byte[] localEngineID = MPv3.createLocalEngineID();
                USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(localEngineID), 0);
                SecurityModels.getInstance().addSecurityModel(usm);
                
                OID authProtocol = getAuthProtocol(device.getAuthProtocol());
                OID privProtocol = getPrivProtocol(device.getPrivProtocol());
                
                UsmUser user = new UsmUser(
                        new OctetString(device.getSecurityName()),
                        authProtocol, new OctetString(device.getAuthPassphrase()),
                        privProtocol, new OctetString(device.getPrivPassphrase())
                );
                
                snmp.getUSM().addUser(new OctetString(device.getSecurityName()), user);

                UserTarget<Address> utarget = new UserTarget<>();
                utarget.setSecurityLevel(getSecurityLevel(device));
                utarget.setSecurityName(new OctetString(device.getSecurityName()));
                target = utarget;
                
                ScopedPDU scopedPdu = new ScopedPDU();
                pdu = scopedPdu;
            } else {
                // Konfiguracja SNMP v1 / v2c
                CommunityTarget<Address> ctarget = new CommunityTarget<>();
                ctarget.setCommunity(new OctetString(device.getCommunity()));
                target = ctarget;
                
                pdu = new PDU();
            }

            Address targetAddress = GenericAddress.parse("udp:" + device.getIpAddress() + "/161");
            target.setAddress(targetAddress);
            target.setRetries(1);
            target.setTimeout(2000);
            target.setVersion(device.getSnmpVersion() == 3 ? SnmpConstants.version3 : 
                             (device.getSnmpVersion() == 0 ? SnmpConstants.version1 : SnmpConstants.version2c));

            pdu.add(new VariableBinding(new OID(oidString)));
            pdu.setType(PDU.GET);

            transport.listen();
            ResponseEvent<Address> response = snmp.get(pdu, target);

            if (response != null && response.getResponse() != null) {
                Variable var = response.getResponse().get(0).getVariable();

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

    private static int getSecurityLevel(Device d) {
        boolean hasAuth = d.getAuthProtocol() != null && !d.getAuthProtocol().equals("NONE");
        boolean hasPriv = d.getPrivProtocol() != null && !d.getPrivProtocol().equals("NONE");
        
        if (hasAuth && hasPriv) return SecurityLevel.AUTH_PRIV;
        if (hasAuth) return SecurityLevel.AUTH_NOPRIV;
        return SecurityLevel.NOAUTH_NOPRIV;
    }

    private static OID getAuthProtocol(String protocol) {
        if (protocol == null) return null;
        return switch (protocol.toUpperCase()) {
            case "MD5" -> AuthMD5.ID;
            case "SHA" -> AuthSHA.ID;
            default -> null;
        };
    }

    private static OID getPrivProtocol(String protocol) {
        if (protocol == null) return null;
        return switch (protocol.toUpperCase()) {
            case "DES" -> PrivDES.ID;
            case "AES", "AES128" -> PrivAES128.ID;
            case "AES192" -> PrivAES192.ID;
            case "AES256" -> PrivAES256.ID;
            default -> null;
        };
    }
}
