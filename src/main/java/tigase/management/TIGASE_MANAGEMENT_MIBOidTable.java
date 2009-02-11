package tigase.management;

//
// Generated by mibgen version 5.1 (03/08/07) when compiling TIGASE-MANAGEMENT-MIB.
//

// java imports
//
import java.io.Serializable;

// jmx imports
//
import com.sun.management.snmp.SnmpOidRecord;

// jdmk imports
//
import com.sun.management.snmp.SnmpOidTableSupport;

/**
 * The class contains metadata definitions for "TIGASE-MANAGEMENT-MIB".
 * Call SnmpOid.setSnmpOidTable(new TIGASE_MANAGEMENT_MIBOidTable()) to load the metadata in the SnmpOidTable.
 */
public class TIGASE_MANAGEMENT_MIBOidTable extends SnmpOidTableSupport implements Serializable {

    /**
     * Default constructor. Initialize the Mib tree.
     */
    public TIGASE_MANAGEMENT_MIBOidTable() {
        super("TIGASE_MANAGEMENT_MIB");
        loadMib(varList);
    }

    static SnmpOidRecord varList [] = {
        new SnmpOidRecord("tigaseSystemUptimeHumanReadable", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.6", "S"),
        new SnmpOidRecord("tigaseSystemUptimeMillis", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.5", "C64"),
        new SnmpOidRecord("tigaseSystemNonHeapUsed", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.4", "C64"),
        new SnmpOidRecord("tigaseSystemNonHeapTotal", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.3", "C64"),
        new SnmpOidRecord("tigaseSystemHeapUsed", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.2", "C64"),
        new SnmpOidRecord("tigaseSystemHeapTotal", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.1", "C64"),
        new SnmpOidRecord("tigaseLoadBoshLastSecond", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.19", "G"),
        new SnmpOidRecord("tigaseLoadC2SLastHour", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.18", "G"),
        new SnmpOidRecord("tigaseLoadC2SLastMinute", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.17", "G"),
        new SnmpOidRecord("tigaseLoadC2SLastSecond", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.16", "G"),
        new SnmpOidRecord("tigaseLoadS2SLastHour", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.15", "G"),
        new SnmpOidRecord("tigaseLoadS2SLastMinute", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.14", "G"),
        new SnmpOidRecord("tigaseLoadS2SLastSecond", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.13", "G"),
        new SnmpOidRecord("tigaseLoadPubSubLastHour", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.12", "G"),
        new SnmpOidRecord("tigaseLoadPubSubLastMinute", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.11", "G"),
        new SnmpOidRecord("tigaseLoadPubSubLastSecond", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.10", "G"),
        new SnmpOidRecord("tigaseLoadMUCLastHour", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.9", "G"),
        new SnmpOidRecord("tigaseLoadMUCLastMinute", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.8", "G"),
        new SnmpOidRecord("tigaseLoadMUCLastSecond", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.7", "G"),
        new SnmpOidRecord("tigaseLoadMRLastHour", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.6", "G"),
        new SnmpOidRecord("tigaseLoadMRLastMinute", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.5", "G"),
        new SnmpOidRecord("tigaseLoadMRLastSecond", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.4", "G"),
        new SnmpOidRecord("tigaseLoadSMLastHour", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.3", "G"),
        new SnmpOidRecord("tigaseLoadSMLastMinute", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.2", "G"),
        new SnmpOidRecord("tigaseLoadSMLastSecond", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.1", "G"),
        new SnmpOidRecord("tigaseLoadBoshLastHour", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.21", "G"),
        new SnmpOidRecord("tigaseLoadBoshLastMinute", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.20", "G"),
        new SnmpOidRecord("tigaseConnectionBoshCount", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.2.3", "G"),
        new SnmpOidRecord("tigaseConnectionServerCount", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.2.2", "G"),
        new SnmpOidRecord("tigaseConnectionClientCount", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.2.1", "G"),
        new SnmpOidRecord("tigaseUserRegisteredCount", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.1.2", "C64"),
        new SnmpOidRecord("tigaseUserSessionCount", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.1.1", "G")    };
}