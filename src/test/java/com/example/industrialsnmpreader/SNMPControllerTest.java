package com.example.industrialsnmpreader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SNMPControllerTest {

    @Test
    void getOidsForVendor_siemens_returnsCorrectOids() {
        SNMPController.VendorOids oids = SNMPController.getOidsForVendor("Siemens");
        assertNotNull(oids);
        assertEquals("1.3.6.1.2.1.1.5.0", oids.hostnameOid);
        assertEquals("1.3.6.1.4.1.261.2.1.1.1.1.0", oids.tempOid);
        assertEquals("1.3.6.1.2.1.1.3.0", oids.uptimeOid);
        assertEquals("1.3.6.1.4.1.261.2.1.1.3.1.0", oids.cpuOid);
    }

    @Test
    void getOidsForVendor_mikrotik_returnsCorrectOids() {
        SNMPController.VendorOids oids = SNMPController.getOidsForVendor("Mikrotik");
        assertNotNull(oids);
        assertEquals("1.3.6.1.2.1.1.5.0", oids.hostnameOid);
        assertEquals("1.3.6.1.4.1.14988.1.1.3.11.0", oids.tempOid);
        assertEquals("1.3.6.1.2.1.1.3.0", oids.uptimeOid);
        assertEquals("1.3.6.1.2.1.25.3.3.1.2.2", oids.cpuOid);
    }

    @Test
    void getOidsForVendor_caseInsensitive() {
        assertNotNull(SNMPController.getOidsForVendor("SIEMENS"));
        assertNotNull(SNMPController.getOidsForVendor("siemens"));
        assertNotNull(SNMPController.getOidsForVendor("MIKROTIK"));
        assertNotNull(SNMPController.getOidsForVendor("mikrotik"));
    }

    @Test
    void getOidsForVendor_unknownVendor_returnsNull() {
        assertNull(SNMPController.getOidsForVendor("cisco"));
        assertNull(SNMPController.getOidsForVendor("unknown"));
        assertNull(SNMPController.getOidsForVendor(""));
    }

    @Test
    void getOidsForVendor_null_returnsNull() {
        assertNull(SNMPController.getOidsForVendor(null));
    }
}
