/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.ndriver.comm.http.HttpComm
 *  com.tridium.ndriver.datatypes.BHttpCommConfig
 *  com.tridium.ndriver.discover.BINDiscoveryHost
 *  com.tridium.ndriver.discover.BINDiscoveryObject
 *  com.tridium.ndriver.discover.BNDiscoveryJob
 *  com.tridium.ndriver.discover.BNDiscoveryPreferences
 *  com.tridium.ndriver.poll.BNPollScheduler
 *  com.tridium.nvideo.BVideoNetwork
 *  javax.baja.license.Feature
 *  javax.baja.naming.BOrd
 *  javax.baja.nre.annotations.NiagaraAction
 *  javax.baja.nre.annotations.NiagaraProperties
 *  javax.baja.nre.annotations.NiagaraProperty
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.nre.util.Array
 *  javax.baja.nre.util.TextUtil
 *  javax.baja.sys.Action
 *  javax.baja.sys.BComplex
 *  javax.baja.sys.BValue
 *  javax.baja.sys.Property
 *  javax.baja.sys.Slot
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 */
package com.tridium.naxisVideo;

import com.tridium.naxisVideo.BAxisVideoCamera;
import com.tridium.naxisVideo.BAxisVideoDeviceFolder;
import com.tridium.naxisVideo.comm.BAxisVideoEventsReceiver;
import com.tridium.naxisVideo.discover.BAxisCameraDiscoveryLeaf;
import com.tridium.naxisVideo.discover.BAxisCameraDiscoveryPreferences;
import com.tridium.naxisVideo.discover.dns.DnsMessage;
import com.tridium.naxisVideo.discover.dns.DnsResourceRecord;
import com.tridium.ndriver.comm.http.HttpComm;
import com.tridium.ndriver.datatypes.BHttpCommConfig;
import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.ndriver.poll.BNPollScheduler;
import com.tridium.nvideo.BVideoNetwork;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.util.logging.Logger;
import javax.baja.license.Feature;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties(value={@NiagaraProperty(name="poll", type="BNPollScheduler", defaultValue="new BNPollScheduler()"), @NiagaraProperty(name="httpConfig", type="BHttpCommConfig", defaultValue="new BHttpCommConfig()"), @NiagaraProperty(name="eventReceiver", type="BAxisVideoEventsReceiver", defaultValue="new BAxisVideoEventsReceiver()"), @NiagaraProperty(name="discoveryPreferences", type="BNDiscoveryPreferences", defaultValue="new BAxisCameraDiscoveryPreferences()")})
@NiagaraAction(name="submitDiscoveryJob", parameterType="BNDiscoveryPreferences", defaultValue="new BAxisCameraDiscoveryPreferences()", returnType="BOrd", flags=4)
public class BAxisVideoNetwork
extends BVideoNetwork
implements BINDiscoveryHost {
    public static final Property poll = BAxisVideoNetwork.newProperty((int)0, (BValue)new BNPollScheduler(), null);
    public static final Property httpConfig = BAxisVideoNetwork.newProperty((int)0, (BValue)new BHttpCommConfig(), null);
    public static final Property eventReceiver = BAxisVideoNetwork.newProperty((int)0, (BValue)new BAxisVideoEventsReceiver(), null);
    public static final Property discoveryPreferences = BAxisVideoNetwork.newProperty((int)0, (BValue)new BAxisCameraDiscoveryPreferences(), null);
    public static final Action submitDiscoveryJob = BAxisVideoNetwork.newAction((int)4, (BValue)new BAxisCameraDiscoveryPreferences(), null);
    public static final Type TYPE = Sys.loadType(BAxisVideoNetwork.class);
    private static final String AXIS_VIDEO_SERVICE = "_axis-video._tcp.local.";
    private static final int PTR_TYPE = 12;
    public Logger axisNetworkLog = Logger.getLogger("NAxisVideo");

    public BNPollScheduler getPoll() {
        return (BNPollScheduler)this.get(poll);
    }

    public void setPoll(BNPollScheduler v) {
        this.set(poll, (BValue)v, null);
    }

    public BHttpCommConfig getHttpConfig() {
        return (BHttpCommConfig)this.get(httpConfig);
    }

    public void setHttpConfig(BHttpCommConfig v) {
        this.set(httpConfig, (BValue)v, null);
    }

    public BAxisVideoEventsReceiver getEventReceiver() {
        return (BAxisVideoEventsReceiver)this.get(eventReceiver);
    }

    public void setEventReceiver(BAxisVideoEventsReceiver v) {
        this.set(eventReceiver, (BValue)v, null);
    }

    public BNDiscoveryPreferences getDiscoveryPreferences() {
        return (BNDiscoveryPreferences)this.get(discoveryPreferences);
    }

    public void setDiscoveryPreferences(BNDiscoveryPreferences v) {
        this.set(discoveryPreferences, (BValue)v, null);
    }

    public BOrd submitDiscoveryJob(BNDiscoveryPreferences parameter) {
        return (BOrd)this.invoke(submitDiscoveryJob, (BValue)parameter, null);
    }

    public Type getType() {
        return TYPE;
    }

    public String getNetworkName() {
        return "axis";
    }

    public Type getDeviceFolderType() {
        return BAxisVideoDeviceFolder.TYPE;
    }

    public Type getDeviceType() {
        return BAxisVideoCamera.TYPE;
    }

    public final Feature getLicenseFeature() {
        BComplex netParent = this.getParent();
        if (netParent != null && netParent.getClass().getName().equals("com.tridium.remoteVideo.BRemoteVideoSource")) {
            return Sys.getLicenseManager().getFeature("tridium", "remoteVideo");
        }
        return Sys.getLicenseManager().getFeature("tridium", "axisVideo");
    }

    public HttpComm hcomm() {
        return (HttpComm)this.getHttpConfig().comm();
    }

    public BOrd doSubmitDiscoveryJob(BNDiscoveryPreferences preferences) {
        this.setDiscoveryPreferences((BNDiscoveryPreferences)preferences.newCopy());
        BNDiscoveryJob discoveryJob = new BNDiscoveryJob((BINDiscoveryHost)this);
        discoveryJob.setDiscoveryPreferences((BNDiscoveryPreferences)preferences.newCopy());
        return discoveryJob.submit(null);
    }

    public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences dp) throws Exception {
        BAxisCameraDiscoveryPreferences prefs = (BAxisCameraDiscoveryPreferences)dp;
        Array discoveryLeaves = new Array(BINDiscoveryObject.class);
        DnsMessage ptr_request = DnsMessage.make_PTR_RequestMessage(AXIS_VIDEO_SERVICE);
        DnsMessage[] responses = ptr_request.send(prefs.getTimeout().getMillis());
        this.axisNetworkLog.finer("No of DnsMessage responses:" + responses.length);
        for (int i = 0; i < responses.length; ++i) {
            BINDiscoveryObject leaf = this.processResponse(responses[i]);
            if (leaf == null) continue;
            discoveryLeaves.add((Object)leaf);
        }
        return (BINDiscoveryObject[])discoveryLeaves.trim();
    }

    private BINDiscoveryObject processResponse(DnsMessage response) throws Exception {
        String serviceName = "";
        boolean isAxisResponse = false;
        boolean isPTRResouceRecord = false;
        DnsResourceRecord[] answers = response.getAnswers();
        this.axisNetworkLog.finer("Dns Messages No of Answers:" + answers.length);
        for (int i = 0; i < answers.length; ++i) {
            int length;
            DnsResourceRecord answer = answers[i];
            byte[] rdata = answer.getResourceData();
            this.axisNetworkLog.finer("Dns Message Answer :DATA_LENGTH>" + answer.getResourceDataLength() + " :DOMAIN_NAME>" + answer.getDomainName() + " :TYPE>" + answer.getType() + " :CLASS>" + answer.getRecordClass() + " :PTRDNAME_LENGTH>" + rdata[0]);
            isAxisResponse = answer.getDomainName().equals(AXIS_VIDEO_SERVICE);
            if (!isAxisResponse) continue;
            boolean bl = isPTRResouceRecord = 12 == answer.getType();
            if (!isPTRResouceRecord || (length = rdata[0]) == 0) continue;
            StringBuilder serviceNameBuilder = new StringBuilder();
            int index = 1;
            int j = 0;
            while (j < length) {
                serviceNameBuilder.append((char)rdata[index]);
                ++j;
                ++index;
            }
            serviceName = TextUtil.replace((String)serviceNameBuilder.toString(), (String)".", (String)"\\.");
            serviceName = TextUtil.replace((String)serviceName, (String)"\\", (String)"\\\\");
        }
        if (!isAxisResponse || !isPTRResouceRecord) {
            return null;
        }
        BAxisCameraDiscoveryLeaf leaf = null;
        DnsResourceRecord[] additionals = response.getAdditionals();
        String ipAddress = null;
        String macAddress = null;
        int port = -1;
        block7: for (int i = 0; i < additionals.length; ++i) {
            DnsResourceRecord additional = additionals[i];
            byte[] resourceData = additional.getResourceData();
            switch (additional.getType()) {
                case 16: {
                    String macAddressPrefix = "macaddress=";
                    String rawTxtString = new String(resourceData);
                    macAddress = rawTxtString.substring(rawTxtString.indexOf(macAddressPrefix) + macAddressPrefix.length());
                    continue block7;
                }
                case 33: {
                    DataInputStream din = new DataInputStream(new ByteArrayInputStream(resourceData));
                    din.readInt();
                    port = din.readUnsignedShort();
                    continue block7;
                }
                case 1: {
                    InetAddress address = InetAddress.getByAddress(resourceData);
                    if (address.isLinkLocalAddress()) continue block7;
                    ipAddress = address.getHostAddress();
                }
            }
        }
        this.axisNetworkLog.finer("DNS additional: IPADDRESS>" + ipAddress + " :MACADDRESS>" + macAddress + " :PORT>" + port + " :SERVICENAME>" + serviceName);
        if (ipAddress != null && macAddress != null && port != -1) {
            leaf = new BAxisCameraDiscoveryLeaf(serviceName, macAddress, ipAddress, port);
        }
        return leaf;
    }

    public void videoNetworkStarted() throws Exception {
        int flag = this.getHttpConfig().getFlags((Slot)BHttpCommConfig.useTls);
        if ((flag & Integer.MIN_VALUE) == 0) {
            this.getHttpConfig().setUseTls(true);
            flag &= 0xFFFFFFFE;
            flag &= 0xFFFFFFFB;
            this.getHttpConfig().setFlags((Slot)BHttpCommConfig.useTls, flag |= Integer.MIN_VALUE);
        }
    }
}

